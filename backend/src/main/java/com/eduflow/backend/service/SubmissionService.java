package com.eduflow.backend.service;
import com.eduflow.backend.dto.PageResponse;
import com.eduflow.backend.dto.AssignmentDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import com.eduflow.backend.repository.*;
import java.util.*;
import java.time.Instant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@Service @Transactional(readOnly=true) @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
public class SubmissionService {
 final AssignmentAccess a;final AssignmentSubmissionRepository submissions;final SubmissionAttachmentRepository attachments;
 final SubmissionRevisionRepository revisions;final SubmissionGradeHistoryRepository grades;final BatchEnrollmentRepository enrollments;
 final AssignmentFiles files;final AssignmentFileValidator validator;
 final NotificationOutbox notifications;
 public SubmissionService(AssignmentAccess a,AssignmentSubmissionRepository submissions,SubmissionAttachmentRepository attachments,SubmissionRevisionRepository revisions,SubmissionGradeHistoryRepository grades,BatchEnrollmentRepository enrollments,AssignmentFiles files,AssignmentFileValidator validator,NotificationOutbox notifications){this.a=a;this.submissions=submissions;this.attachments=attachments;this.revisions=revisions;this.grades=grades;this.enrollments=enrollments;this.files=files;this.validator=validator;this.notifications=notifications;}
 private AssignmentSubmission locked(Long id){var s=a.find(AssignmentSubmission.class,id,"Submission",false);a.assignment(s.getAssignment().getId(),true);a.em.refresh(s);return s;}
 private void writable(Assignment assignment,Student student,boolean replacing){
  writable(assignment,student,replacing,a.now());
 }
 private void writable(Assignment assignment,Student student,boolean replacing,Instant acceptedAt){
  if(assignment.getStatus()!=AssignmentStatus.PUBLISHED)throw new AssignmentConflictException("Assignment is not open for submissions");
  if(!a.recipients.existsByAssignmentIdAndStudentId(assignment.getId(),student.getId()))throw new AccessDeniedException("You are not an eligible recipient of this assignment");
  var date=acceptedAt.atZone(AssignmentAccess.ZONE).toLocalDate();
  boolean enrolled=enrollments.findByBatchIdAndStudentIdOrderByEnrollmentDate(assignment.getBatch().getId(),student.getId()).stream().anyMatch(n->!n.getEnrollmentDate().isAfter(date)&&(n.getWithdrawalDate()==null||n.getWithdrawalDate().isAfter(date))&&(n.getCompletionDate()==null||!n.getCompletionDate().isBefore(date)));
  if(!enrolled)throw new AccessDeniedException("A valid current enrollment is required to submit work");
  boolean late=!acceptedAt.isBefore(assignment.getDueDateTime());
  if(late&&(replacing||!assignment.getAllowLateSubmission()))throw new AssignmentConflictException(replacing?"Resubmission is allowed only before the deadline":"The submission deadline has passed");
 }
 private void editable(AssignmentSubmission s,Long version){a.owner(s);a.version(s.getVersion(),version);if(s.getStatus()==SubmissionStatus.GRADED)throw new AssignmentConflictException("Graded work cannot be edited");writable(s.getAssignment(),s.getStudent(),true);}
 private List<AssignmentFileValidator.ValidatedFile> validateFiles(List<MultipartFile> uploads){
  if(uploads==null)return List.of();if(uploads.size()>10)throw new IllegalArgumentException("Maximum ten files per submission");
  return uploads.stream().map(validator::validate).toList();
 }
 @PreAuthorize("hasRole('STUDENT')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public SubmissionView create(Long assignmentId,SubmissionRequest input,List<MultipartFile> uploads){
  var assignment=a.assignment(assignmentId,true);var student=a.own();writable(assignment,student,false);
  if(submissions.findByAssignmentIdAndStudentId(assignmentId,student.getId()).isPresent())throw new AssignmentConflictException("You already submitted this assignment. Open your existing submission to revise it");
  if(input.keepAttachmentIds()!=null&&!input.keepAttachmentIds().isEmpty())throw new IllegalArgumentException("Existing files cannot be linked to a new submission");
  var validated=validateFiles(uploads);String text=clean(input.submissionText());if(text.isEmpty()&&validated.isEmpty())throw new IllegalArgumentException("Enter submission text or attach at least one valid file");
  Instant acceptedAt=a.now();writable(assignment,student,false,acceptedAt);
  var s=new AssignmentSubmission();s.setAssignment(assignment);s.setStudent(student);s.setSubmissionText(text);s.setSubmittedAt(acceptedAt);s.setLate(!acceptedAt.isBefore(assignment.getDueDateTime()));s.setStatus(s.getLate()?SubmissionStatus.LATE:SubmissionStatus.SUBMITTED);submissions.saveAndFlush(s);append(s,validated);a.em.flush();return view(s);
 }
 @PreAuthorize("hasRole('STUDENT')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public SubmissionView update(Long id,SubmissionRequest input,List<MultipartFile> uploads){
  var s=locked(id);editable(s,input.version());var old=attachments.findBySubmissionIdAndActiveTrueOrderById(id);var validated=validateFiles(uploads);
  Set<Long> keep=input.keepAttachmentIds()==null?new HashSet<>(old.stream().map(SubmissionAttachment::getId).toList()):new HashSet<>(input.keepAttachmentIds());
  if(input.keepAttachmentIds()!=null&&keep.size()!=input.keepAttachmentIds().size())throw new IllegalArgumentException("Duplicate attachment IDs");
  if(!old.stream().map(SubmissionAttachment::getId).toList().containsAll(keep))throw new IllegalArgumentException("Only your currently active attachments can be retained");
  if(keep.size()+validated.size()>10)throw new IllegalArgumentException("Maximum ten active files per submission");
  String text=clean(input.submissionText());if(text.isEmpty()&&keep.isEmpty()&&validated.isEmpty())throw new IllegalArgumentException("A submission cannot be empty");
  Instant acceptedAt=a.now();writable(s.getAssignment(),s.getStudent(),true,acceptedAt);
  revision(s,old);for(var f:old)if(!keep.contains(f.getId()))f.setActive(false);
  s.setSubmissionText(text);s.setSubmittedAt(acceptedAt);s.setLate(false);s.setStatus(SubmissionStatus.SUBMITTED);append(s,validated);a.bump(s);return view(s);
 }
 private void append(AssignmentSubmission s,List<AssignmentFileValidator.ValidatedFile> validated){for(var f:validated){var row=new SubmissionAttachment();row.setSubmission(s);row.setOriginalFileName(f.name());row.setStorageKey(files.save(f.bytes()));row.setContentType(f.contentType());row.setFileSize((long)f.bytes().length);attachments.save(row);}}
 private void revision(AssignmentSubmission s,List<SubmissionAttachment> old){
  var r=new SubmissionRevision();r.setSubmission(s);r.setRevisionNumber(s.getVersion());r.setSubmissionText(s.getSubmissionText());r.setSubmittedAt(s.getSubmittedAt());r.setStatus(s.getStatus());r.setAttachmentIds(old.stream().map(x->x.getId().toString()).collect(java.util.stream.Collectors.joining(",")));r.setChangedBy(a.user());r.setChangedAt(a.now());revisions.save(r);
 }
 @PreAuthorize("hasRole('STUDENT')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public List<FileView> upload(Long id,Long version,List<MultipartFile> uploads){
  var s=locked(id);editable(s,version);var old=attachments.findBySubmissionIdAndActiveTrueOrderById(id);
  if(uploads==null||uploads.isEmpty()||old.size()+uploads.size()>10)throw new IllegalArgumentException("Upload 1–10 files; maximum ten active files per submission");
  var validated=validateFiles(uploads);Instant acceptedAt=a.now();writable(s.getAssignment(),s.getStudent(),true,acceptedAt);revision(s,old);append(s,validated);s.setSubmittedAt(acceptedAt);a.bump(s);return attachments(id);
 }
 public SubmissionView get(Long id){var s=a.find(AssignmentSubmission.class,id,"Submission",false);a.submission(s);return view(s);}
 @PreAuthorize("hasRole('STUDENT')") public SubmissionView ownForAssignment(Long id){var s=a.own();return submissions.findByAssignmentIdAndStudentId(id,s.getId()).map(this::view).orElseThrow(()->new ResourceNotFoundException("You have not submitted this assignment"));}
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") public PageResponse<SubmissionView> list(Long id,int page,int size,String search,SubmissionStatus status){
  var assignment=a.assignment(id,false);a.manage(assignment);CourseService.validatePage(page,size,search);String pattern=CourseService.pattern(search);
  return PageResponse.from(submissions.findAll((r,q,cb)->{var p=cb.and(cb.equal(r.get("assignment").get("id"),id),cb.or(cb.like(cb.lower(r.get("student").get("fullName")),pattern,'\\'),cb.like(cb.lower(r.get("student").get("admissionNumber")),pattern,'\\')));if(status!=null)p=cb.and(p,cb.equal(r.get("status"),status));return p;},FinanceService.paging(page,size,"submittedAt,desc",Set.of("submittedAt"))).map(this::view));
 }
 @PreAuthorize("hasRole('STUDENT')") public PageResponse<SubmissionView> mine(int page,int size,SubmissionStatus status){
  var student=a.students.findByUserEmail(a.academic.email());FinanceService.paging(page,size,"submittedAt,desc",Set.of("submittedAt"));
  if(student.isEmpty())return new PageResponse<>(List.of(),page,size,0,0);
  return PageResponse.from(submissions.findAll((r,q,cb)->{var p=cb.equal(r.get("student").get("id"),student.get().getId());if(status!=null)p=cb.and(p,cb.equal(r.get("status"),status));return p;},FinanceService.paging(page,size,"submittedAt,desc",Set.of("submittedAt"))).map(this::view));
 }
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public SubmissionView grade(Long id,GradeRequest input){
  var s=locked(id);a.manage(s.getAssignment());a.version(s.getVersion(),input.version());var marks=AssignmentAccess.marks(input.obtainedMarks());if(marks.compareTo(s.getAssignment().getMaxMarks())>0)throw new IllegalArgumentException("Obtained marks exceed assignment maximum");
  boolean correction=s.getStatus()==SubmissionStatus.GRADED;
  if(correction&&clean(input.reason()).isEmpty())throw new IllegalArgumentException("A reason is required to correct an existing grade");
  var history=new SubmissionGradeHistory();history.setSubmission(s);history.setPreviousMarks(s.getObtainedMarks());history.setNewMarks(marks);history.setPreviousFeedback(s.getFeedback());history.setNewFeedback(clean(input.feedback()));history.setReason(correction?input.reason().strip():"Initial grading");history.setGradedBy(a.user());history.setGradedAt(a.now());grades.save(history);
  s.setObtainedMarks(marks);s.setFeedback(history.getNewFeedback());s.setGradedBy(history.getGradedBy());s.setGradedAt(history.getGradedAt());s.setStatus(SubmissionStatus.GRADED);a.em.flush();notifications.capture("assignment-grade:"+id+":"+history.getId(),NotificationType.ASSIGNMENT_GRADED,"SUBMISSION",id,null);return view(s);
 }
 public List<FileView> attachments(Long id){var s=a.find(AssignmentSubmission.class,id,"Submission",false);a.submission(s);return attachments.findBySubmissionIdAndActiveTrueOrderById(id).stream().map(this::fileView).toList();}
 public AssignmentService.Download download(Long id){var file=a.find(SubmissionAttachment.class,id,"Attachment",false);a.submission(file.getSubmission());return new AssignmentService.Download(file.getOriginalFileName(),file.getContentType(),files.read(file.getStorageKey()));}
 public History history(Long id){
  var s=a.find(AssignmentSubmission.class,id,"Submission",false);a.submission(s);
  Map<Long,SubmissionAttachment> all=new HashMap<>();attachments.findBySubmissionIdOrderById(id).forEach(f->all.put(f.getId(),f));
  var old=revisions.findBySubmissionIdOrderByRevisionNumberDesc(id).stream().map(r->new RevisionView(r.getId(),r.getRevisionNumber(),r.getSubmissionText(),r.getSubmittedAt(),r.getStatus(),r.getChangedBy().getFullName(),r.getChangedAt(),r.getAttachmentIds().isEmpty()?List.of():Arrays.stream(r.getAttachmentIds().split(",")).map(Long::valueOf).map(all::get).filter(Objects::nonNull).map(this::fileView).toList())).toList();
  var grading=grades.findBySubmissionIdOrderByGradedAtDescIdDesc(id).stream().map(g->new GradeHistoryView(g.getId(),g.getPreviousMarks(),g.getNewMarks(),g.getPreviousFeedback(),g.getNewFeedback(),g.getReason(),g.getGradedBy().getFullName(),g.getGradedAt())).toList();
  return new History(old,grading);
 }
 public SubmissionView view(AssignmentSubmission s){
  var assignment=s.getAssignment();var batch=assignment.getBatch();var student=s.getStudent();
  return new SubmissionView(s.getId(),assignment.getId(),assignment.getTitle(),batch.getName(),batch.getCourse().getName(),student.getId(),student.getFullName(),student.getAdmissionNumber(),s.getSubmissionText(),s.getSubmittedAt(),s.getStatus(),s.getLate(),assignment.getMaxMarks(),s.getObtainedMarks(),s.getFeedback(),s.getGradedBy()==null?null:s.getGradedBy().getFullName(),s.getGradedAt(),s.getVersion(),attachments.findBySubmissionIdAndActiveTrueOrderById(s.getId()).stream().map(this::fileView).toList());
 }
 FileView fileView(SubmissionAttachment file){return new FileView(file.getId(),file.getOriginalFileName(),file.getContentType(),file.getFileSize(),file.getCreatedAt());}
 private static String clean(String value){return value==null?"":value.strip();}
}
