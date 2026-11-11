package com.eduflow.backend.service;
import com.eduflow.backend.dto.AssignmentDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.repository.*;
import java.math.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true) @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
public class AssignmentStatisticsService {
 final NamedParameterJdbcTemplate jdbc;final AssignmentAccess a;final AssignmentRepository assignments;final AssignmentSubmissionRepository submissions;final AssignmentService service;final SubmissionService submissionService;
 public AssignmentStatisticsService(NamedParameterJdbcTemplate jdbc,AssignmentAccess a,AssignmentRepository assignments,AssignmentSubmissionRepository submissions,AssignmentService service,SubmissionService submissionService){this.jdbc=jdbc;this.a=a;this.assignments=assignments;this.submissions=submissions;this.service=service;this.submissionService=submissionService;}
 public Statistics stats(Long batchId){
  Map<String,Object> p=new HashMap<>();p.put("email",a.academic.email());p.put("now",java.sql.Timestamp.from(a.now()));
  String base=" from assignments w join batches b on b.id=w.batch_id ",scope=" where 1=1 ";
  String own="(select s.id from students s join users u on u.id=s.user_id where u.email=:email)";
  if(a.academic.hasRole("TEACHER"))scope+=" and exists(select 1 from batch_teacher_assignments ta join teachers t on t.id=ta.teacher_id join users u on u.id=t.user_id where ta.batch_id=w.batch_id and ta.active=true and u.email=:email)";
  if(a.student())scope+=" and exists(select 1 from assignment_recipients r where r.assignment_id=w.id and r.student_id="+own+") and (w.status='PUBLISHED' or (w.status='CLOSED' and exists(select 1 from assignment_submissions ss where ss.assignment_id=w.id and ss.student_id="+own+")))";
  if(batchId!=null){scope+=" and w.batch_id=:batch";p.put("batch",batchId);}
  long total=count(base+scope,p),published=count(base+scope+" and w.status='PUBLISHED'",p),created=count(base+scope+" and w.created_by_id=(select u.id from users u where u.email=:email)",p);
  String cohort=scope+" and w.status in ('PUBLISHED','CLOSED') ";
  if(a.student())cohort+=" and r.student_id="+own;
  String join=base+" join assignment_recipients r on r.assignment_id=w.id left join assignment_submissions ss on ss.assignment_id=w.id and ss.student_id=r.student_id ";
  long eligible=count(join+cohort,p),submitted=count(join+cohort+" and ss.id is not null",p),pending=count(join+cohort+" and w.status='PUBLISHED' and ss.id is null",p),
   late=count(join+cohort+" and ss.late=true",p),graded=count(join+cohort+" and ss.status='GRADED'",p);
  long overdue=assignments.count((w,q,cb)->{
   var waiting=q.subquery(Long.class);var r=waiting.from(AssignmentRecipient.class);
   var existing=waiting.subquery(Long.class);var s=existing.from(AssignmentSubmission.class);
   existing.select(s.get("id")).where(cb.equal(s.get("assignment").get("id"),w.get("id")),cb.equal(s.get("student").get("id"),r.get("student").get("id")));
   var match=cb.and(cb.equal(r.get("assignment").get("id"),w.get("id")),cb.not(cb.exists(existing)));
   if(a.student())match=cb.and(match,cb.equal(r.get("student").get("user").get("email"),a.academic.email()));
   waiting.select(r.get("id")).where(match);
   var visible=cb.and(a.scope(w,q,cb),cb.equal(w.get("status"),AssignmentStatus.PUBLISHED),cb.lessThanOrEqualTo(w.get("dueDateTime"),a.now()),cb.exists(waiting));
   if(batchId!=null)visible=cb.and(visible,cb.equal(w.get("batch").get("id"),batchId));return visible;
  });
  BigDecimal avg=jdbc.queryForObject("select avg(ss.obtained_marks)"+join+cohort+" and ss.status='GRADED'",p,BigDecimal.class);
  BigDecimal avgPercent=jdbc.queryForObject("select avg(100.0*ss.obtained_marks/w.max_marks)"+join+cohort+" and ss.status='GRADED'",p,BigDecimal.class);
  var batches=jdbc.query("select b.id,b.name,count(r.id),count(ss.id)"+join+cohort+" group by b.id,b.name order by b.name,b.id limit 100",p,
   (rs,n)->new BatchCompletion(rs.getLong(1),rs.getString(2),rs.getLong(3),rs.getLong(4),AssignmentAccess.percent(rs.getLong(4),rs.getLong(3))));
  var today=a.now().atZone(AssignmentAccess.ZONE).toLocalDate();p.put("activityFrom",today.minusDays(29).atStartOfDay());p.put("activityTo",today.plusDays(1).atStartOfDay());
  var activity=jdbc.query("select cast(ss.created_at as date),b.id,b.name,count(ss.id)"+join+cohort+" and ss.id is not null and ss.created_at>=:activityFrom and ss.created_at<:activityTo group by 1,b.id,b.name order by 1 desc,b.id limit 300",p,
   (rs,n)->new BatchActivity(rs.getDate(1).toLocalDate(),rs.getLong(2),rs.getString(3),rs.getLong(4)));
  var next=assignments.findAll((r,q,cb)->{var match=cb.and(a.scope(r,q,cb),cb.equal(r.get("status"),AssignmentStatus.PUBLISHED),cb.greaterThan(r.get("dueDateTime"),a.now()));if(batchId!=null)match=cb.and(match,cb.equal(r.get("batch").get("id"),batchId));if(a.student()){var done=q.subquery(Long.class);var s=done.from(AssignmentSubmission.class);done.select(s.get("id")).where(cb.equal(s.get("assignment").get("id"),r.get("id")),cb.equal(s.get("student").get("user").get("email"),a.academic.email()));match=cb.and(match,cb.not(cb.exists(done)));}return match;},PageRequest.of(0,5,Sort.by("dueDateTime").and(Sort.by("id")))).map(service::view).getContent();
  var recent=submissions.findAll((r,q,cb)->{jakarta.persistence.criteria.Join<AssignmentSubmission,Assignment> w=r.join("assignment");var match=a.scope(w,q,cb);if(a.student())match=cb.and(match,cb.equal(r.get("student").get("user").get("email"),a.academic.email()));if(batchId!=null)match=cb.and(match,cb.equal(w.get("batch").get("id"),batchId));return match;},PageRequest.of(0,5,Sort.by("updatedAt").descending().and(Sort.by("id")))).map(submissionService::view).getContent();
  return new Statistics(total,published,created,eligible,pending,submitted,late,graded,overdue,AssignmentAccess.percent(submitted,eligible),AssignmentAccess.percent(graded,submitted),scaled(avg),scaled(avgPercent),batches,next,recent,activity,a.now());
 }
 private long count(String sql,Map<String,Object> p){return jdbc.queryForObject("select count(*)"+sql,p,Long.class);}
 private BigDecimal scaled(BigDecimal v){return v==null?null:v.setScale(2,RoundingMode.HALF_UP);}
}
