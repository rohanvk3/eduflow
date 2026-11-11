package com.eduflow.backend.service;
import com.eduflow.backend.dto.PageResponse;
import com.eduflow.backend.dto.ExamDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import com.eduflow.backend.repository.*;
import java.math.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @Transactional(readOnly=true) @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
public class ExamResultService {
 final ExamAccess a;final ExamService exams;final ExamResultRepository results;final ExamAttemptRepository attempts;
 final ExamAttemptService attemptService;final ExamResultCorrectionRepository corrections;final ExamAnswerRepository answers;final ExamQuestionRepository questions;final ExamOptionRepository options;
 final NotificationOutbox notifications;
 public ExamResultService(ExamAccess a,ExamService exams,ExamResultRepository results,ExamAttemptRepository attempts,ExamAttemptService attemptService,
  ExamResultCorrectionRepository corrections,ExamAnswerRepository answers,ExamQuestionRepository questions,ExamOptionRepository options,NotificationOutbox notifications){this.a=a;this.exams=exams;this.results=results;this.attempts=attempts;this.attemptService=attemptService;this.corrections=corrections;this.answers=answers;this.questions=questions;this.options=options;this.notifications=notifications;}
 private void offline(Exam e){a.manage(e);if(e.getExamType()!=ExamType.OFFLINE)throw new IllegalArgumentException("Not an offline exam");}
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") public Gradebook gradebook(Long id){var e=a.exam(id,false);offline(e);var map=new HashMap<Long,ExamResult>();results.findByExamId(id).forEach(r->map.put(r.getStudent().getId(),r));
  return new Gradebook(exams.view(e),a.roster(e).stream().map(s->{var r=map.get(s.getId());return new GradebookRow(s.getId(),s.getAdmissionNumber(),s.getFullName(),r==null?null:r.getObtainedMarks(),r==null?"":r.getRemarks());}).toList());
 }
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public Gradebook saveOffline(Long id,OfflineRequest request){
  var e=a.exam(id,true);offline(e);a.version(e,request.version());if(e.getStatus()!=ExamStatus.PUBLISHED||e.getResultsPublished())throw new ExamConflictException("Marks entry requires an exam with unpublished results");
  if(a.now().isBefore(e.getEndDateTime()))throw new ExamConflictException("Marks entry opens after the exam ends");
  var eligible=new HashMap<Long,Student>();a.roster(e).forEach(s->eligible.put(s.getId(),s));var seen=new HashSet<Long>();
  for(var input:request.results()){if(!seen.add(input.studentId()))throw new IllegalArgumentException("Duplicate student");if(!eligible.containsKey(input.studentId()))throw new IllegalArgumentException("Student is not eligible for this exam");validMarks(e,input.obtainedMarks());}
  for(var input:request.results()){var r=results.findByExamIdAndStudentId(id,input.studentId()).orElseGet(ExamResult::new);r.setExam(e);r.setStudent(eligible.get(input.studentId()));r.setObtainedMarks(ExamAccess.marks(input.obtainedMarks()));r.setRemarks(input.remarks());r.setEvaluatedBy(a.user());r.setEvaluatedAt(a.now());results.save(r);}
  a.bump(e);return gradebook(id);
 }
 private void validMarks(Exam e,BigDecimal marks){ExamAccess.marks(marks);if(marks.compareTo(e.getTotalMarks())>0)throw new IllegalArgumentException("Obtained marks exceed exam total");}
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public ExamView publish(Long id,VersionRequest request){
  var e=a.exam(id,true);a.manage(e);a.version(e,request.version());if(e.getStatus()!=ExamStatus.PUBLISHED||e.getResultsPublished())throw new ExamConflictException("Results are not awaiting publication");
  if(a.now().isBefore(e.getEndDateTime()))throw new ExamConflictException("Results cannot publish before exam availability ends");
  var eligible=a.roster(e);if(eligible.isEmpty())throw new ExamConflictException("No eligible students for result publication");
  if(e.getExamType()==ExamType.ONLINE_MCQ){for(var t:attempts.findByExamId(id)){attemptService.expireIfDue(t);if(t.getStatus()==ExamAttemptStatus.IN_PROGRESS)throw new ExamConflictException("An attempt remains active");}
   if(results.findByExamId(id).isEmpty())throw new ExamConflictException("No completed attempts to publish");
  }else{
   var ids=new HashSet<Long>();results.findByExamId(id).forEach(r->ids.add(r.getStudent().getId()));
   if(eligible.stream().anyMatch(s->!ids.contains(s.getId())))throw new ExamConflictException("Enter marks for every eligible student, including explicit zero for absence");
  }
  e.setResultsPublished(true);e.setResultsPublishedAt(a.now());e.setResultsPublishedBy(a.user());e.setStatus(ExamStatus.COMPLETED);a.em.flush();notifications.capture("exam-results:"+id,NotificationType.EXAM_RESULT_PUBLISHED,"EXAM",id,null);return exams.view(e);
 }
 public OwnResult ownResult(Long id){
  var e=a.exam(id,false);a.read(e);if(!a.student())throw new AccessDeniedException("This endpoint is for student results");
  if(!e.getResultsPublished())return new OwnResult(false,null,List.of());var s=a.own();var result=results.findByExamIdAndStudentId(id,s.getId());
  if(result.isEmpty())return new OwnResult(true,null,List.of());
  var review=new ArrayList<ReviewQuestion>();
  if(e.getExamType()==ExamType.ONLINE_MCQ){var t=attempts.findByExamIdAndStudentId(id,s.getId()).orElseThrow();var saved=new HashMap<Long,ExamAnswer>();answers.findByAttemptId(t.getId()).forEach(x->saved.put(x.getQuestion().getId(),x));
   for(var q:questions.findByExamIdOrderByDisplayOrderAscIdAsc(id)){var opts=options.findByQuestionIdOrderByDisplayOrder(q.getId());var answer=saved.get(q.getId());review.add(new ReviewQuestion(q.getId(),q.getQuestionText(),q.getMarks(),opts.stream().map(o->new StudentOption(o.getId(),o.getOptionText())).toList(),
    answer==null||answer.getSelectedOption()==null?null:answer.getSelectedOption().getId(),opts.stream().filter(ExamOption::getCorrect).findFirst().orElseThrow().getId(),answer==null?BigDecimal.ZERO:answer.getAwardedMarks()));}
  }
  return new OwnResult(true,view(result.get()),review);
 }
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") public Results examResults(Long id,int page,int size){
  var e=a.exam(id,false);a.manage(e);if(!e.getResultsPublished())throw new ExamConflictException("Results are not published");
  var list=results.findByExamId(id);return new Results(exams.view(e),summary(list),PageResponse.from(results.findAll((r,q,cb)->cb.equal(r.get("exam").get("id"),id),
   FinanceService.paging(page,size,"obtainedMarks,desc",Set.of("obtainedMarks"))).map(this::view)));
 }
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") public PageResponse<RankedResult> leaderboard(Long id,int page,int size){
  var e=a.exam(id,false);a.manage(e);if(!e.getResultsPublished())throw new ExamConflictException("Results are not published");
  FinanceService.paging(page,size,"id,asc",Set.of("id"));var sorted=results.findByExamId(id).stream().sorted(Comparator.comparing(ExamResult::getObtainedMarks).reversed().thenComparing(ExamResult::getId)).toList();
  List<RankedResult> rows=new ArrayList<>();BigDecimal prior=null;long rank=0;for(int i=0;i<sorted.size();i++){var r=sorted.get(i);if(prior==null||r.getObtainedMarks().compareTo(prior)!=0)rank=i+1;rows.add(new RankedResult(rank,view(r)));prior=r.getObtainedMarks();}
  int start=(int)Math.min((long)page*size,rows.size());return new PageResponse<>(rows.subList(start,Math.min(start+size,rows.size())),page,size,rows.size(),(rows.size()+size-1)/size);
 }
 public PageResponse<ResultView> studentResults(Long studentId,int page,int size){
  if(a.student()){var s=a.own();if(studentId!=null&&!studentId.equals(s.getId()))throw new AccessDeniedException("Only your own results are accessible");studentId=s.getId();}
  if(studentId==null)throw new IllegalArgumentException("Student is required");final Long sid=studentId;
  return PageResponse.from(results.findAll((r,q,cb)->cb.and(cb.equal(r.get("student").get("id"),sid),cb.isTrue(r.get("exam").get("resultsPublished")),
   a.scope(r.join("exam"),q,cb)),FinanceService.paging(page,size,"evaluatedAt,desc",Set.of("evaluatedAt"))).map(this::view));
 }
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public ResultView correct(Long id,Long resultId,CorrectionRequest input){
  var e=a.exam(id,true);offline(e);a.version(e,input.version());if(!e.getResultsPublished())throw new ExamConflictException("Use draft marks entry for unpublished results");
  var r=a.find(ExamResult.class,resultId,"Result",false);if(!r.getExam().getId().equals(id))throw new IllegalArgumentException("Result belongs to another exam");validMarks(e,input.obtainedMarks());
  var audit=new ExamResultCorrection();audit.setResult(r);audit.setPreviousMarks(r.getObtainedMarks());audit.setNewMarks(ExamAccess.marks(input.obtainedMarks()));audit.setReason(input.reason().strip());audit.setCorrectedBy(a.user());audit.setCorrectedAt(a.now());corrections.save(audit);
  r.setObtainedMarks(audit.getNewMarks());r.setEvaluatedBy(a.user());r.setEvaluatedAt(a.now());a.bump(e);return view(r);
 }
 @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") public List<CorrectionView> audit(Long id,Long resultId){var e=a.exam(id,false);a.manage(e);var r=a.find(ExamResult.class,resultId,"Result",false);if(!r.getExam().getId().equals(id))throw new IllegalArgumentException("Result belongs to another exam");return corrections.findByResultIdOrderByCorrectedAt(resultId).stream().map(x->new CorrectionView(x.getId(),x.getPreviousMarks(),x.getNewMarks(),x.getReason(),x.getCorrectedBy().getFullName(),x.getCorrectedAt())).toList();}
 public ResultView view(ExamResult r){var e=r.getExam();BigDecimal p=percent(r.getObtainedMarks(),e.getTotalMarks());return new ResultView(r.getId(),e.getId(),e.getTitle(),e.getBatch().getName(),e.getBatch().getCourse().getName(),e.getStartDateTime(),r.getStudent().getId(),r.getStudent().getFullName(),r.getStudent().getAdmissionNumber(),r.getObtainedMarks(),e.getTotalMarks(),p,r.getObtainedMarks().compareTo(e.getPassingMarks())>=0,grade(p),r.getEvaluatedAt(),r.getRemarks());}
 public static BigDecimal percent(BigDecimal score,BigDecimal total){return total.signum()==0?null:score.multiply(BigDecimal.valueOf(100)).divide(total,2,RoundingMode.HALF_UP);}
 public static String grade(BigDecimal p){if(p==null)return "N/A";int[] cut={90,80,70,60,50};String[] labels={"A+","A","B","C","D"};for(int i=0;i<cut.length;i++)if(p.compareTo(BigDecimal.valueOf(cut[i]))>=0)return labels[i];return "F";}
 ResultSummary summary(List<ExamResult> rows){if(rows.isEmpty())return new ResultSummary(0,null,null,null,null);var scores=rows.stream().map(ExamResult::getObtainedMarks).toList();BigDecimal sum=scores.stream().reduce(BigDecimal.ZERO,BigDecimal::add);return new ResultSummary(rows.size(),sum.divide(BigDecimal.valueOf(rows.size()),2,RoundingMode.HALF_UP),scores.stream().max(BigDecimal::compareTo).orElseThrow(),scores.stream().min(BigDecimal::compareTo).orElseThrow(),BigDecimal.valueOf(rows.stream().filter(r->r.getObtainedMarks().compareTo(r.getExam().getPassingMarks())>=0).count()*100).divide(BigDecimal.valueOf(rows.size()),2,RoundingMode.HALF_UP));}
}
