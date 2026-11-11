package com.eduflow.backend.service;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import com.eduflow.backend.repository.*;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import java.time.*;
import java.math.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
@Component
public class ExamAccess {
 public static final ZoneId ZONE=ZoneId.of("Asia/Kolkata");
 final AcademicAccess access;final StudentRepository students;final BatchEnrollmentRepository enrollments;
 final UserRepository users;final EntityManager em;final Clock clock;
 public ExamAccess(AcademicAccess access,StudentRepository students,BatchEnrollmentRepository enrollments,UserRepository users,EntityManager em,@Qualifier("examClock") Clock clock){
  this.access=access;this.students=students;this.enrollments=enrollments;this.users=users;this.em=em;this.clock=clock;
 }
 public Instant now(){return clock.instant();}
 public boolean staff(){return access.hasRole("ADMIN")||access.hasRole("TEACHER");}
 public boolean student(){return access.hasRole("STUDENT");}
 public Student own(){return students.findByUserEmail(access.email()).orElseThrow(()->new ResourceNotFoundException("Your login is not linked to a student record"));}
 public User user(){return users.findByEmail(access.email()).orElseThrow(()->new AccessDeniedException("User unavailable"));}
 public <T>T find(Class<T> type,Long id,String label,boolean lock){var result=lock?em.find(type,id,LockModeType.PESSIMISTIC_WRITE):em.find(type,id);if(result==null)throw new ResourceNotFoundException(label+" not found");return result;}
 public Exam exam(Long id,boolean lock){return find(Exam.class,id,"Exam",lock);}
 public void manage(Exam e){if(!staff())throw new AccessDeniedException("Exam management denied");access.readRoster(e.getBatch().getId());}
 public List<Student> roster(Exam e){Map<Long,Student> unique=new LinkedHashMap<>();for(var n:enrollments.findEligibleOn(e.getBatch().getId(),e.getEligibilityDate()))unique.put(n.getStudent().getId(),n.getStudent());return unique.values().stream().sorted(Comparator.comparing(Student::getFullName).thenComparing(Student::getId)).toList();}
 public boolean eligible(Exam e,Student s){return enrollments.findByBatchIdAndStudentIdOrderByEnrollmentDate(e.getBatch().getId(),s.getId()).stream().anyMatch(n->!n.getEnrollmentDate().isAfter(e.getEligibilityDate())&&(n.getWithdrawalDate()==null||n.getWithdrawalDate().isAfter(e.getEligibilityDate()))&&(n.getCompletionDate()==null||!n.getCompletionDate().isBefore(e.getEligibilityDate())));}
 public void read(Exam e){if(staff()){manage(e);return;}if(!student()||e.getStatus()==ExamStatus.DRAFT||e.getStatus()==ExamStatus.CANCELLED||!eligible(e,own()))throw new AccessDeniedException("Exam access denied");}
 public void version(Exam e,Long version){if(version==null||version!=e.getVersion())throw new ExamConflictException("Exam changed; reload before retrying");}
 public void draft(Exam e){if(e.getStatus()!=ExamStatus.DRAFT)throw new ExamConflictException("Published exam rules and questions cannot be modified");}
 public void bump(Exam e){em.lock(e,LockModeType.PESSIMISTIC_FORCE_INCREMENT);em.flush();}
 public static BigDecimal marks(BigDecimal value){if(value==null||value.signum()<0||value.compareTo(new BigDecimal("99999999.99"))>0)throw new IllegalArgumentException("Invalid marks");try{return value.setScale(2,RoundingMode.UNNECESSARY);}catch(ArithmeticException ex){throw new IllegalArgumentException("Marks require at most two decimal places");}}
 public Predicate scope(From<?,Exam> r,CriteriaQuery<?> q,CriteriaBuilder cb){
  if(access.hasRole("ADMIN"))return cb.conjunction();
  if(access.hasRole("TEACHER")){
   var sub=q.subquery(Long.class);var a=sub.from(BatchTeacherAssignment.class);sub.select(a.get("batch").get("id")).where(cb.isTrue(a.get("active")),cb.equal(a.get("teacher").get("user").get("email"),access.email()));return r.get("batch").get("id").in(sub);
  }
  if(!student())return cb.disjunction();
  var sub=q.subquery(Long.class);var n=sub.from(BatchEnrollment.class);sub.select(n.get("batch").get("id")).where(cb.equal(n.get("student").get("user").get("email"),access.email()),
   cb.lessThanOrEqualTo(n.get("enrollmentDate"),r.get("eligibilityDate")),cb.or(cb.isNull(n.get("withdrawalDate")),cb.greaterThan(n.get("withdrawalDate"),r.get("eligibilityDate"))),
   cb.or(cb.isNull(n.get("completionDate")),cb.greaterThanOrEqualTo(n.get("completionDate"),r.get("eligibilityDate"))));
  return cb.and(r.get("batch").get("id").in(sub),r.get("status").in(ExamStatus.PUBLISHED,ExamStatus.COMPLETED));
 }
}
