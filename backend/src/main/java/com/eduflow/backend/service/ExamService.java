package com.eduflow.backend.service;
import com.eduflow.backend.dto.PageResponse;
import com.eduflow.backend.dto.ExamDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import com.eduflow.backend.repository.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @Transactional(readOnly=true) @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
public class ExamService {
 final ExamRepository exams;final ExamQuestionRepository questions;final ExamOptionRepository options;
 final ExamAttemptRepository attempts;final ExamResultRepository results;final ExamAccess a;
 final NotificationOutbox notifications;
 public ExamService(ExamRepository exams,ExamQuestionRepository questions,ExamOptionRepository options,ExamAttemptRepository attempts,ExamResultRepository results,ExamAccess a,NotificationOutbox notifications){this.exams=exams;this.questions=questions;this.options=options;this.attempts=attempts;this.results=results;this.a=a;this.notifications=notifications;}
 public PageResponse<ExamView> list(int page,int size,String search,Long batchId,ExamType type,ExamStatus status,String sort){
  CourseService.validatePage(page,size,search);var pattern=CourseService.pattern(search);
  return PageResponse.from(exams.findAll((r,q,cb)->{
   var p=cb.and(a.scope(r,q,cb),cb.or(cb.like(cb.lower(r.get("title")),pattern,'\\'),cb.like(cb.lower(r.get("examCode")),pattern,'\\')));
   if(batchId!=null)p=cb.and(p,cb.equal(r.get("batch").get("id"),batchId));if(type!=null)p=cb.and(p,cb.equal(r.get("examType"),type));if(status!=null)p=cb.and(p,cb.equal(r.get("status"),status));return p;
  },FinanceService.paging(page,size,sort,Set.of("id","title","startDateTime","status","examCode"))).map(this::view));
 }
 public ExamView get(Long id){var e=a.exam(id,false);a.read(e);return view(e);}
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public ExamView save(Long id,ExamRequest r){
  var e=id==null?new Exam():a.exam(id,true);if(id!=null){a.manage(e);a.draft(e);a.version(e,r.version());}
  var batch=a.find(Batch.class,r.batchId(),"Batch",false);a.access.readRoster(batch.getId());a.access.editable(batch);
  if(!r.endDateTime().isAfter(r.startDateTime()))throw new IllegalArgumentException("Exam end must be after start");
  if(!r.endDateTime().isAfter(a.now()))throw new IllegalArgumentException("New exam availability must end in the future");
  if(r.examType()==ExamType.ONLINE_MCQ&&(r.durationMinutes()==null||r.durationMinutes()<1))throw new IllegalArgumentException("Online duration must be positive");
  var start=r.startDateTime().atZone(ExamAccess.ZONE).toLocalDate();var end=r.endDateTime().atZone(ExamAccess.ZONE).toLocalDate();
  if(start.isBefore(batch.getStartDate())||end.isAfter(batch.getEndDate()))throw new IllegalArgumentException("Exam must fall within the batch dates");
  if(r.passingMarks().compareTo(r.totalMarks())>0)throw new IllegalArgumentException("Passing marks exceed total marks");
  if(id!=null&&!questions.findByExamIdOrderByDisplayOrderAscIdAsc(id).isEmpty()&&r.examType()!=e.getExamType())throw new ExamConflictException("Remove draft questions before changing exam type");
  String code=r.examCode().toUpperCase(Locale.ROOT);if(exams.existsByExamCodeIgnoreCaseAndIdNot(code,id==null?0L:id))throw new ExamConflictException("Exam code already exists");
  e.setExamCode(code);e.setTitle(r.title().strip());e.setDescription(r.description());e.setBatch(batch);e.setExamType(r.examType());e.setDurationMinutes(r.durationMinutes());e.setTotalMarks(ExamAccess.marks(r.totalMarks()));e.setPassingMarks(ExamAccess.marks(r.passingMarks()));e.setStartDateTime(r.startDateTime());e.setEndDateTime(r.endDateTime());e.setEligibilityDate(start);if(id==null)e.setCreatedBy(a.user());exams.saveAndFlush(e);return view(e);
 }
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public ExamView publish(Long id,VersionRequest r){var e=a.exam(id,true);a.manage(e);a.version(e,r.version());a.draft(e);a.access.editable(e.getBatch());
  if(!a.now().isBefore(e.getEndDateTime()))throw new ExamConflictException("Exam availability has ended");
  if(e.getExamType()==ExamType.ONLINE_MCQ){var qs=questions.findByExamIdOrderByDisplayOrderAscIdAsc(id);if(qs.isEmpty())throw new IllegalArgumentException("Online exams need questions");
   BigDecimal total=BigDecimal.ZERO;for(var q:qs){validateOptions(options.findByQuestionIdOrderByDisplayOrder(q.getId()).stream().map(o->new OptionInput(o.getOptionText(),o.getCorrect())).toList());total=total.add(q.getMarks());}
   if(total.compareTo(e.getTotalMarks())!=0)throw new IllegalArgumentException("Question marks must equal exam total");}
  e.setStatus(ExamStatus.PUBLISHED);exams.flush();notifications.capture("exam-published:"+id,NotificationType.EXAM_SCHEDULED,"EXAM",id,null);return view(e);
 }
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public ExamView cancel(Long id,CancelRequest r){var e=a.exam(id,true);a.manage(e);a.version(e,r.version());if(e.getStatus()==ExamStatus.COMPLETED||e.getStatus()==ExamStatus.CANCELLED||!attempts.findByExamId(id).isEmpty()||!results.findByExamId(id).isEmpty())throw new ExamConflictException("Exams with assessment history cannot be cancelled");e.setStatus(ExamStatus.CANCELLED);e.setCancellationReason(r.reason().strip());exams.flush();return view(e);}
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") public List<StaffQuestion> questions(Long id){var e=a.exam(id,false);a.manage(e);return questions.findByExamIdOrderByDisplayOrderAscIdAsc(id).stream().map(this::questionView).toList();}
 static void validateOptions(List<OptionInput> opts){if(opts==null||opts.size()<2||opts.size()>10||opts.stream().filter(OptionInput::correct).count()!=1)throw new IllegalArgumentException("Each question needs 2–10 options and exactly one correct answer");}
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public StaffQuestion saveQuestion(Long id,Long qid,QuestionRequest r){var e=a.exam(id,true);a.manage(e);a.draft(e);a.version(e,r.version());if(e.getExamType()!=ExamType.ONLINE_MCQ)throw new IllegalArgumentException("Offline exams do not have MCQs");validateOptions(r.options());
  var all=questions.findByExamIdOrderByDisplayOrderAscIdAsc(id);if(qid==null&&all.size()>=200)throw new IllegalArgumentException("Maximum 200 questions per exam");
  if(all.stream().anyMatch(q->!q.getId().equals(qid)&&q.getDisplayOrder().equals(r.displayOrder())))throw new IllegalArgumentException("Display order already used; use reorder");
  var q=qid==null?new ExamQuestion():a.find(ExamQuestion.class,qid,"Question",false);if(qid!=null&&!q.getExam().getId().equals(id))throw new IllegalArgumentException("Question belongs to another exam");
  q.setExam(e);q.setQuestionText(r.questionText().strip());q.setMarks(ExamAccess.marks(r.marks()));q.setDisplayOrder(r.displayOrder());questions.saveAndFlush(q);
  if(qid!=null){options.deleteByQuestionId(qid);options.flush();}int order=0;for(var input:r.options()){var o=new ExamOption();o.setQuestion(q);o.setOptionText(input.optionText().strip());o.setCorrect(input.correct());o.setDisplayOrder(++order);options.save(o);}options.flush();a.bump(e);return questionView(q);
 }
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public void deleteQuestion(Long id,Long qid,Long version){var e=a.exam(id,true);a.manage(e);a.draft(e);a.version(e,version);var q=a.find(ExamQuestion.class,qid,"Question",false);if(!q.getExam().getId().equals(id))throw new IllegalArgumentException("Question belongs to another exam");options.deleteByQuestionId(qid);options.flush();questions.delete(q);a.bump(e);}
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public List<StaffQuestion> reorder(Long id,OrderRequest r){var e=a.exam(id,true);a.manage(e);a.draft(e);a.version(e,r.version());var all=questions.findByExamIdOrderByDisplayOrderAscIdAsc(id);var map=new HashMap<Long,ExamQuestion>();all.forEach(q->map.put(q.getId(),q));if(r.questionIds().size()!=map.size()||!new HashSet<>(r.questionIds()).equals(map.keySet()))throw new IllegalArgumentException("Reorder must contain every question exactly once");int i=0;for(var qid:r.questionIds())map.get(qid).setDisplayOrder(++i);a.bump(e);return questions(id);}
 StaffQuestion questionView(ExamQuestion q){return new StaffQuestion(q.getId(),q.getQuestionText(),q.getMarks(),q.getDisplayOrder(),options.findByQuestionIdOrderByDisplayOrder(q.getId()).stream().map(o->new StaffOption(o.getId(),o.getOptionText(),o.getCorrect())).toList());}
 public ExamView view(Exam e){return new ExamView(e.getId(),e.getExamCode(),e.getTitle(),e.getDescription(),e.getBatch().getId(),e.getBatch().getName(),e.getBatch().getCourse().getName(),e.getExamType(),e.getDurationMinutes(),e.getTotalMarks(),e.getPassingMarks(),e.getStartDateTime(),e.getEndDateTime(),e.getStatus(),e.getResultsPublished(),e.getResultsPublishedAt(),e.getVersion());}
}
