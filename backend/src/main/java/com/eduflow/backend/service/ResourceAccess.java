package com.eduflow.backend.service;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import jakarta.persistence.criteria.*;
import java.time.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
@Component
public class ResourceAccess {
 final AssignmentAccess shared;
 public ResourceAccess(AssignmentAccess shared){this.shared=shared;}
 public boolean admin(){return shared.academic.hasRole("ADMIN");}
 public boolean staff(){return shared.staff();}
 public User user(){return shared.user();}
 public <T>T find(Class<T> type,Long id,String label,boolean lock){return shared.find(type,id,label,lock);}
 public void manage(Course course,Batch batch){
  if(admin())return;
  if(!staff()||batch==null)throw new AccessDeniedException("Only administrators can manage course-wide resources");
  shared.academic.readRoster(batch.getId());
 }
 public void read(Course course,Batch batch){
  if(admin())return;
  if(staff()){
   Long count=shared.em.createQuery("select count(a) from BatchTeacherAssignment a where a.active=true and a.teacher.user.email=:email and a.batch.course.id=:course and (:batch is null or a.batch.id=:batch)",Long.class).setParameter("email",shared.academic.email()).setParameter("course",course.getId()).setParameter("batch",batch==null?null:batch.getId()).getSingleResult();
   if(count>0)return;
  }else if(shared.student()){
   LocalDate day=shared.now().atZone(AssignmentAccess.ZONE).toLocalDate();
   Long count=shared.em.createQuery("select count(n) from BatchEnrollment n where n.student.user.email=:email and n.batch.course.id=:course and (:batch is null or n.batch.id=:batch) and n.status=:status and n.enrollmentDate<=:day and (n.withdrawalDate is null or n.withdrawalDate>:day) and (n.completionDate is null or n.completionDate>=:day)",Long.class).setParameter("email",shared.academic.email()).setParameter("course",course.getId()).setParameter("batch",batch==null?null:batch.getId()).setParameter("status",EnrollmentStatus.ACTIVE).setParameter("day",day).getSingleResult();
   if(count>0)return;
  }
  throw new AccessDeniedException("Resource access denied");
 }
 public void read(StudyMaterial m){read(m.getCourse(),m.getBatch());if(!staff()&&m.getVisibility()!=ResourceVisibility.PUBLISHED)throw new AccessDeniedException("Resource is not published");}
 public Predicate scope(From<?,?> r,CriteriaQuery<?> q,CriteriaBuilder cb){
  if(admin())return cb.conjunction();
  if(!staff()&&!shared.student())return cb.disjunction();
  var batches=q.subquery(Long.class);var courses=q.subquery(Long.class);
  if(staff()){
   var a=batches.from(BatchTeacherAssignment.class);batches.select(a.get("batch").get("id")).where(cb.isTrue(a.get("active")),cb.equal(a.get("teacher").get("user").get("email"),shared.academic.email()));
   var b=courses.from(BatchTeacherAssignment.class);courses.select(b.get("batch").get("course").get("id")).where(cb.isTrue(b.get("active")),cb.equal(b.get("teacher").get("user").get("email"),shared.academic.email()));
  }else{
   var n=batches.from(BatchEnrollment.class);batches.select(n.get("batch").get("id")).where(enrollment(n,cb));
   var c=courses.from(BatchEnrollment.class);courses.select(c.get("batch").get("course").get("id")).where(enrollment(c,cb));
  }
  return cb.or(cb.and(cb.isNull(r.get("batch")),r.get("course").get("id").in(courses)),r.get("batch").get("id").in(batches));
 }
 private Predicate enrollment(From<?,BatchEnrollment> n,CriteriaBuilder cb){
  var day=shared.now().atZone(AssignmentAccess.ZONE).toLocalDate();
  return cb.and(cb.equal(n.get("student").get("user").get("email"),shared.academic.email()),cb.equal(n.get("status"),EnrollmentStatus.ACTIVE),cb.lessThanOrEqualTo(n.get("enrollmentDate"),day),cb.or(cb.isNull(n.get("withdrawalDate")),cb.greaterThan(n.get("withdrawalDate"),day)),cb.or(cb.isNull(n.get("completionDate")),cb.greaterThanOrEqualTo(n.get("completionDate"),day)));
 }
 public Batch batch(Long id,Course course){if(id==null)return null;var b=find(Batch.class,id,"Batch",false);if(!b.getCourse().getId().equals(course.getId()))throw new IllegalArgumentException("Batch must belong to the selected course");return b;}
 public void unchanged(long current,Long requested){shared.version(current,requested);}
 public void conflict(String message){throw new AcademicConflictException(message);}
}
