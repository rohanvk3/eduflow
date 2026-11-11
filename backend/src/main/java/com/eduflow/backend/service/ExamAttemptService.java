package com.eduflow.backend.service;
import com.eduflow.backend.dto.ExamDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import com.eduflow.backend.repository.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @Transactional(readOnly=true)
public class ExamAttemptService {
 final ExamAccess a;final ExamAttemptRepository attempts;final ExamQuestionRepository questions;final ExamOptionRepository options;
 final ExamAnswerRepository answers;final ExamResultRepository results;
 public ExamAttemptService(ExamAccess a,ExamAttemptRepository attempts,ExamQuestionRepository questions,ExamOptionRepository options,ExamAnswerRepository answers,ExamResultRepository results){this.a=a;this.attempts=attempts;this.questions=questions;this.options=options;this.answers=answers;this.results=results;}
 @PreAuthorize("hasRole('STUDENT')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public AttemptView start(Long id){
  var e=a.exam(id,true);a.read(e);var s=a.own();if(e.getExamType()!=ExamType.ONLINE_MCQ)throw new IllegalArgumentException("Not an online exam");
  var existing=attempts.findByExamIdAndStudentId(id,s.getId());if(existing.isPresent()){var t=existing.get();expireIfDue(t);return view(t,true);}
  if(e.getStatus()!=ExamStatus.PUBLISHED||a.now().isBefore(e.getStartDateTime())||!a.now().isBefore(e.getEndDateTime()))throw new ExamConflictException("Exam is not available now");
  var t=new ExamAttempt();t.setExam(e);t.setStudent(s);t.setStartedAt(a.now());Instant limit=t.getStartedAt().plusSeconds(e.getDurationMinutes()*60L);t.setExpiresAt(limit.isBefore(e.getEndDateTime())?limit:e.getEndDateTime());attempts.saveAndFlush(t);return view(t,true);
 }
 private ExamAttempt locked(Long id){var t=a.find(ExamAttempt.class,id,"Attempt",false);a.exam(t.getExam().getId(),true);a.em.refresh(t);return t;}
 private void own(ExamAttempt t){if(!t.getStudent().getId().equals(a.own().getId()))throw new AccessDeniedException("Only your own attempt is accessible");}
 @PreAuthorize("hasRole('STUDENT')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public AttemptView get(Long id){var t=locked(id);own(t);expireIfDue(t);return view(t,true);}
 @PreAuthorize("hasRole('STUDENT')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public AttemptView save(Long id,SaveAnswers r){
  var t=locked(id);own(t);if(t.getStatus()!=ExamAttemptStatus.IN_PROGRESS)throw new ExamConflictException("Submitted attempts are immutable");
  if(expireIfDue(t))return view(t,false);
  if(t.getVersion()!=r.version())throw new ExamConflictException("Attempt changed in another tab; reload your saved answers");
  Set<Long> seen=new HashSet<>();Map<Long,ExamQuestion> qs=new HashMap<>();for(var q:questions.findByExamIdOrderByDisplayOrderAscIdAsc(t.getExam().getId()))qs.put(q.getId(),q);
  Map<Long,ExamOption> selected=new HashMap<>();
  for(var input:r.answers()){
   if(!seen.add(input.questionId()))throw new IllegalArgumentException("Duplicate question in answer submission");
   if(!qs.containsKey(input.questionId()))throw new IllegalArgumentException("Question is not part of this exam");
   if(input.selectedOptionId()!=null){var o=a.find(ExamOption.class,input.selectedOptionId(),"Option",false);if(!o.getQuestion().getId().equals(input.questionId()))throw new IllegalArgumentException("Option is not part of this question");selected.put(input.questionId(),o);}
  }
  Map<Long,ExamAnswer> old=new HashMap<>();answers.findByAttemptId(id).forEach(x->old.put(x.getQuestion().getId(),x));
  for(var input:r.answers()){var x=old.getOrDefault(input.questionId(),new ExamAnswer());x.setAttempt(t);x.setQuestion(qs.get(input.questionId()));x.setSelectedOption(selected.get(input.questionId()));answers.save(x);}
  a.em.lock(t,jakarta.persistence.LockModeType.PESSIMISTIC_FORCE_INCREMENT);a.em.flush();return view(t,true);
 }
 @PreAuthorize("hasRole('STUDENT')") @Transactional(isolation=Isolation.READ_COMMITTED)
 public AttemptView submit(Long id){var t=locked(id);own(t);if(t.getStatus()==ExamAttemptStatus.IN_PROGRESS)evaluate(t,!a.now().isBefore(t.getExpiresAt()));return view(t,true);}
 /** Internal scheduler entry point. No HTTP endpoint exposes this method. */
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void expire(Long id){var t=locked(id);expireIfDue(t);}
 boolean expireIfDue(ExamAttempt t){if(t.getStatus()==ExamAttemptStatus.IN_PROGRESS&&!a.now().isBefore(t.getExpiresAt())){evaluate(t,true);return true;}return false;}
 void evaluate(ExamAttempt t,boolean expired){
  if(t.getStatus()!=ExamAttemptStatus.IN_PROGRESS)return;
  Map<Long,ExamAnswer> saved=new HashMap<>();answers.findByAttemptId(t.getId()).forEach(x->saved.put(x.getQuestion().getId(),x));
  BigDecimal score=new BigDecimal("0.00");
  for(var q:questions.findByExamIdOrderByDisplayOrderAscIdAsc(t.getExam().getId())){
   var x=saved.getOrDefault(q.getId(),new ExamAnswer());x.setAttempt(t);x.setQuestion(q);
   BigDecimal awarded=x.getSelectedOption()!=null&&x.getSelectedOption().getCorrect()?q.getMarks():new BigDecimal("0.00");x.setAwardedMarks(awarded);answers.save(x);score=score.add(awarded);
  }
  if(score.compareTo(t.getExam().getTotalMarks())>0)throw new IllegalStateException("Invalid question totals");
  t.setObtainedMarks(score);t.setSubmittedAt(expired?t.getExpiresAt():a.now());t.setStatus(expired?ExamAttemptStatus.AUTO_SUBMITTED:ExamAttemptStatus.SUBMITTED);
  if(results.findByExamIdAndStudentId(t.getExam().getId(),t.getStudent().getId()).isEmpty()){
   var r=new ExamResult();r.setExam(t.getExam());r.setStudent(t.getStudent());r.setObtainedMarks(score);r.setEvaluatedAt(a.now());r.setRemarks(expired?"Automatically finalized at the server deadline":"Automatically evaluated");results.save(r);
  }
  a.em.flush();
 }
 AttemptView view(ExamAttempt t,boolean accepted){
  var qs=questions.findByExamIdOrderByDisplayOrderAscIdAsc(t.getExam().getId()).stream().map(q->new StudentQuestion(q.getId(),q.getQuestionText(),q.getMarks(),q.getDisplayOrder(),
    options.findByQuestionIdOrderByDisplayOrder(q.getId()).stream().map(o->new StudentOption(o.getId(),o.getOptionText())).toList())).toList();
  var saved=answers.findByAttemptId(t.getId()).stream().map(x->new SavedAnswer(x.getQuestion().getId(),x.getSelectedOption()==null?null:x.getSelectedOption().getId())).toList();
  return new AttemptView(t.getId(),t.getExam().getId(),t.getExam().getTitle(),t.getStatus(),t.getStartedAt(),t.getExpiresAt(),a.now(),t.getVersion(),accepted,qs,saved);
 }
}
