package com.eduflow.backend.service;
import com.eduflow.backend.dto.PageResponse;
import com.eduflow.backend.dto.AssignmentDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import com.eduflow.backend.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@Service @Transactional(readOnly=true) @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
public class AssignmentService {
 final AssignmentAccess a;final AssignmentRepository assignments;final AssignmentRecipientRepository recipients;
 final AssignmentSubmissionRepository submissions;final AssignmentAttachmentRepository attachments;
 final BatchEnrollmentRepository enrollments;final AssignmentFileValidator validator;final AssignmentFiles files;
 final NotificationOutbox notifications;
 public AssignmentService(AssignmentAccess a,AssignmentRepository assignments,AssignmentRecipientRepository recipients,AssignmentSubmissionRepository submissions,
  AssignmentAttachmentRepository attachments,BatchEnrollmentRepository enrollments,AssignmentFileValidator validator,AssignmentFiles files,NotificationOutbox notifications){this.a=a;this.assignments=assignments;this.recipients=recipients;this.submissions=submissions;this.attachments=attachments;this.enrollments=enrollments;this.validator=validator;this.files=files;this.notifications=notifications;}
 public PageResponse<AssignmentView> list(int page,int size,String search,Long batchId,AssignmentStatus status,LocalDate from,LocalDate to,String sort){
  return list(page,size,search,batchId,status,from,to,sort,false);
 }
 public PageResponse<AssignmentView> list(int page,int size,String search,Long batchId,AssignmentStatus status,LocalDate from,LocalDate to,String sort,boolean pendingOnly){
  if(pendingOnly&&!a.student())throw new IllegalArgumentException("The pending-homework filter is for student accounts");
  CourseService.validatePage(page,size,search);FinanceService.dateRange(from,to);String pattern=CourseService.pattern(search);
  return PageResponse.from(assignments.findAll((r,q,cb)->{var p=cb.and(a.scope(r,q,cb),cb.like(cb.lower(r.get("title")),pattern,'\\'));
   if(batchId!=null)p=cb.and(p,cb.equal(r.get("batch").get("id"),batchId));if(status!=null)p=cb.and(p,cb.equal(r.get("status"),status));
   if(from!=null)p=cb.and(p,cb.greaterThanOrEqualTo(r.get("dueDateTime"),from.atStartOfDay(AssignmentAccess.ZONE).toInstant()));
   if(to!=null)p=cb.and(p,cb.lessThan(r.get("dueDateTime"),to.plusDays(1).atStartOfDay(AssignmentAccess.ZONE).toInstant()));
   if(pendingOnly){var done=q.subquery(Long.class);var s=done.from(AssignmentSubmission.class);done.select(s.get("id")).where(cb.equal(s.get("assignment").get("id"),r.get("id")),cb.equal(s.get("student").get("user").get("email"),a.academic.email()));p=cb.and(p,cb.equal(r.get("status"),AssignmentStatus.PUBLISHED),cb.not(cb.exists(done)));}
   return p;
  },FinanceService.paging(page,size,sort,Set.of("id","title","dueDateTime","status","maxMarks"))).map(this::view));
 }
 public AssignmentView get(Long id){var assignment=a.assignment(id,false);a.read(assignment);return view(assignment);}
 @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
 public AssignmentView save(Long id,AssignmentRequest input){
  var assignment=id==null?new Assignment():a.assignment(id,true);if(id!=null){a.manage(assignment);a.draft(assignment);a.version(assignment.getVersion(),input.version());}
  var batch=a.find(Batch.class,input.batchId(),"Batch",false);a.academic.readRoster(batch.getId());a.academic.editable(batch);
  if(!input.dueDateTime().isAfter(a.now()))throw new IllegalArgumentException("Due date must be in the future");
  if(input.maxMarks().signum()<=0)throw new IllegalArgumentException("Maximum marks must be positive");
  assignment.setTitle(input.title().strip());assignment.setDescription(input.description());assignment.setBatch(batch);assignment.setMaxMarks(AssignmentAccess.marks(input.maxMarks()));assignment.setDueDateTime(input.dueDateTime());assignment.setAllowLateSubmission(input.allowLateSubmission());if(id==null)assignment.setCreatedBy(a.user());assignments.saveAndFlush(assignment);return view(assignment);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
 public AssignmentView publish(Long id,VersionRequest input){
  var assignment=a.assignment(id,true);a.manage(assignment);a.draft(assignment);a.version(assignment.getVersion(),input.version());a.academic.editable(assignment.getBatch());
  Instant publicationTime=a.now();
  if(!assignment.getDueDateTime().isAfter(publicationTime))throw new AssignmentConflictException("Due date must be after publication time");
  var roster=new LinkedHashMap<Long,Student>();enrollments.findEligibleOn(assignment.getBatch().getId(),publicationTime.atZone(AssignmentAccess.ZONE).toLocalDate()).forEach(e->roster.put(e.getStudent().getId(),e.getStudent()));
  if(roster.isEmpty())throw new AssignmentConflictException("Enroll at least one eligible student before publishing");
  for(var student:roster.values()){var recipient=new AssignmentRecipient();recipient.setAssignment(assignment);recipient.setStudent(student);recipients.save(recipient);}
  assignment.setPublishedAt(publicationTime);assignment.setStatus(AssignmentStatus.PUBLISHED);a.em.flush();notifications.capture("assignment-published:"+id,NotificationType.ASSIGNMENT_CREATED,"ASSIGNMENT",id,null);return view(assignment);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
 public AssignmentView close(Long id,VersionRequest input){
  var assignment=a.assignment(id,true);a.manage(assignment);a.version(assignment.getVersion(),input.version());
  if(assignment.getStatus()!=AssignmentStatus.PUBLISHED)throw new AssignmentConflictException("Only a published assignment can be closed");assignment.setStatus(AssignmentStatus.CLOSED);a.em.flush();return view(assignment);
 }
 public Counts counts(Assignment assignment){
  long eligible=recipients.countByAssignmentId(assignment.getId()),submitted=submissions.countByAssignmentId(assignment.getId());
  return new Counts(eligible,submitted,Math.max(0,eligible-submitted),submissions.countByAssignmentIdAndLateTrue(assignment.getId()),submissions.countByAssignmentIdAndStatus(assignment.getId(),SubmissionStatus.GRADED),AssignmentAccess.percent(submitted,eligible));
 }
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")public PageResponse<RecipientView> pendingRecipients(Long id,int page,int size,String search){
  var assignment=a.assignment(id,false);a.manage(assignment);CourseService.validatePage(page,size,search);String pattern=CourseService.pattern(search);
  return PageResponse.from(recipients.findAll((r,q,cb)->{var sub=q.subquery(Long.class);var s=sub.from(AssignmentSubmission.class);sub.select(s.get("student").get("id")).where(cb.equal(s.get("assignment").get("id"),id));
   return cb.and(cb.equal(r.get("assignment").get("id"),id),cb.not(r.get("student").get("id").in(sub)),cb.or(cb.like(cb.lower(r.get("student").get("fullName")),pattern,'\\'),cb.like(cb.lower(r.get("student").get("admissionNumber")),pattern,'\\')));
  },FinanceService.paging(page,size,"id,asc",Set.of("id"))).map(r->new RecipientView(r.getStudent().getId(),r.getStudent().getFullName(),r.getStudent().getAdmissionNumber())));
 }
 public AssignmentView view(Assignment assignment){
  var b=assignment.getBatch();var own=a.student()?a.students.findByUserEmail(a.academic.email()).flatMap(s->submissions.findByAssignmentIdAndStudentId(assignment.getId(),s.getId())).orElse(null):null;
  return new AssignmentView(assignment.getId(),assignment.getTitle(),assignment.getDescription(),b.getId(),b.getName(),b.getCourse().getName(),assignment.getCreatedBy().getFullName(),assignment.getMaxMarks(),assignment.getPublishedAt(),assignment.getDueDateTime(),assignment.getAllowLateSubmission(),assignment.getStatus(),assignment.getVersion(),a.now(),counts(assignment),own==null?null:own.getId(),own==null?null:own.getStatus());
 }
 public List<FileView> attachments(Long id){var assignment=a.assignment(id,false);a.read(assignment);return attachments.findByAssignmentIdOrderById(id).stream().map(this::fileView).toList();}
 @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
 public List<FileView> upload(Long id,Long version,List<MultipartFile> uploads){
  var assignment=a.assignment(id,true);a.manage(assignment);a.draft(assignment);a.version(assignment.getVersion(),version);
  if(uploads==null||uploads.isEmpty()||uploads.size()+attachments.findByAssignmentIdOrderById(id).size()>10)throw new IllegalArgumentException("An assignment may have at most ten files");
  var validated=uploads.stream().map(validator::validate).toList();
  for(var file:validated){var item=new AssignmentAttachment();item.setAssignment(assignment);item.setOriginalFileName(file.name());item.setContentType(file.contentType());item.setFileSize((long)file.bytes().length);item.setStorageKey(files.save(file.bytes()));item.setUploadedBy(a.user());attachments.save(item);}a.bump(assignment);return attachments(id);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
 public void removeAttachment(Long id,Long version){
  var item=a.find(AssignmentAttachment.class,id,"Attachment",false);var assignment=a.assignment(item.getAssignment().getId(),true);a.manage(assignment);a.draft(assignment);a.version(assignment.getVersion(),version);attachments.delete(item);files.deleteAfterCommit(item.getStorageKey());a.bump(assignment);
 }
 public record Download(String name,String contentType,byte[] bytes){}
 public Download download(Long id){var item=a.find(AssignmentAttachment.class,id,"Attachment",false);a.read(item.getAssignment());return new Download(item.getOriginalFileName(),item.getContentType(),files.read(item.getStorageKey()));}
 FileView fileView(AssignmentAttachment item){return new FileView(item.getId(),item.getOriginalFileName(),item.getContentType(),item.getFileSize(),item.getCreatedAt());}
}
