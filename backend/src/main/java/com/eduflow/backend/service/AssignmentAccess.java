package com.eduflow.backend.service;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import com.eduflow.backend.repository.*;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import java.math.*;
import java.time.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
@Component
public class AssignmentAccess {
 static final ZoneId ZONE=ZoneId.of("Asia/Kolkata");
 final AcademicAccess academic;final UserRepository users;final StudentRepository students;
 final AssignmentRecipientRepository recipients;final AssignmentSubmissionRepository submissions;final EntityManager em;final Clock clock;
 public AssignmentAccess(AcademicAccess academic,UserRepository users,StudentRepository students,AssignmentRecipientRepository recipients,AssignmentSubmissionRepository submissions,EntityManager em,@Qualifier("assignmentClock")Clock clock){this.academic=academic;this.users=users;this.students=students;this.recipients=recipients;this.submissions=submissions;this.em=em;this.clock=clock;}
 public Instant now(){return clock.instant();}
 public boolean staff(){return academic.hasRole("ADMIN")||academic.hasRole("TEACHER");}
 public boolean student(){return academic.hasRole("STUDENT");}
 public Student own(){return students.findByUserEmail(academic.email()).orElseThrow(()->new ResourceNotFoundException("Your login is not linked to a student record"));}
 public User user(){return users.findByEmail(academic.email()).orElseThrow(()->new AccessDeniedException("User unavailable"));}
 public <T>T find(Class<T> type,Long id,String label,boolean lock){var value=lock?em.find(type,id,LockModeType.PESSIMISTIC_WRITE):em.find(type,id);if(value==null)throw new ResourceNotFoundException(label+" not found");return value;}
 public Assignment assignment(Long id,boolean lock){return find(Assignment.class,id,"Assignment",lock);}
 public void manage(Assignment assignment){if(!staff())throw new AccessDeniedException("Assignment management denied");academic.readRoster(assignment.getBatch().getId());}
 public void read(Assignment assignment){
  if(staff()){manage(assignment);return;}
  if(!student())throw new AccessDeniedException("Assignment access denied");var s=own();
  if(recipients.existsByAssignmentIdAndStudentId(assignment.getId(),s.getId())&&(assignment.getStatus()==AssignmentStatus.PUBLISHED||(assignment.getStatus()==AssignmentStatus.CLOSED&&submissions.findByAssignmentIdAndStudentId(assignment.getId(),s.getId()).isPresent())))return;
  throw new AccessDeniedException("Assignment access denied");
 }
 public void submission(AssignmentSubmission s){if(staff()){manage(s.getAssignment());return;}if(student()&&s.getStudent().getUser()!=null&&s.getStudent().getUser().getEmail().equals(academic.email()))return;throw new AccessDeniedException("Submission access denied");}
 public void owner(AssignmentSubmission s){if(!student()||!s.getStudent().getId().equals(own().getId()))throw new AccessDeniedException("Only your own work can be submitted");}
 public void draft(Assignment assignment){if(assignment.getStatus()!=AssignmentStatus.DRAFT)throw new AssignmentConflictException("Published assignment rules and teaching attachments are immutable");}
 public void version(long current,Long requested){if(requested==null||current!=requested)throw new AssignmentConflictException("The record changed; reload before retrying");}
 public void bump(Object entity){em.lock(entity,LockModeType.PESSIMISTIC_FORCE_INCREMENT);em.flush();}
 public static BigDecimal marks(BigDecimal value){if(value==null||value.signum()<0||value.compareTo(new BigDecimal("99999999.99"))>0)throw new IllegalArgumentException("Invalid marks");try{return value.setScale(2,RoundingMode.UNNECESSARY);}catch(ArithmeticException ex){throw new IllegalArgumentException("Marks require at most two decimal places");}}
 public static BigDecimal percent(long n,long denominator){return denominator==0?null:BigDecimal.valueOf(n).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominator),2,RoundingMode.HALF_UP);}
 public Predicate scope(From<?,Assignment> root,CriteriaQuery<?> query,CriteriaBuilder cb){
  if(academic.hasRole("ADMIN"))return cb.conjunction();
  if(academic.hasRole("TEACHER")){var sub=query.subquery(Long.class);var t=sub.from(BatchTeacherAssignment.class);sub.select(t.get("batch").get("id")).where(cb.isTrue(t.get("active")),cb.equal(t.get("teacher").get("user").get("email"),academic.email()));return root.get("batch").get("id").in(sub);}
  if(!student())return cb.disjunction();
  var roster=query.subquery(Long.class);var r=roster.from(AssignmentRecipient.class);roster.select(r.get("assignment").get("id")).where(cb.equal(r.get("student").get("user").get("email"),academic.email()));
  var history=query.subquery(Long.class);var s=history.from(AssignmentSubmission.class);history.select(s.get("assignment").get("id")).where(cb.equal(s.get("student").get("user").get("email"),academic.email()));
  return cb.and(root.get("id").in(roster),cb.or(cb.equal(root.get("status"),AssignmentStatus.PUBLISHED),cb.and(cb.equal(root.get("status"),AssignmentStatus.CLOSED),root.get("id").in(history))));
 }
}
