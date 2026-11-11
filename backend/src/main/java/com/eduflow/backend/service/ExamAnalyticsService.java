package com.eduflow.backend.service;
import com.eduflow.backend.dto.ExamDtos.*;
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
public class ExamAnalyticsService {
 final NamedParameterJdbcTemplate jdbc;final ExamAccess a;final ExamRepository exams;final ExamResultRepository results;final ExamService service;final ExamResultService resultService;
 public ExamAnalyticsService(NamedParameterJdbcTemplate jdbc,ExamAccess a,ExamRepository exams,ExamResultRepository results,ExamService service,ExamResultService resultService){this.jdbc=jdbc;this.a=a;this.exams=exams;this.results=results;this.service=service;this.resultService=resultService;}
 public Analytics analytics(Long batchId,Long studentId){
  Map<String,Object> p=new HashMap<>();p.put("now",java.sql.Timestamp.from(a.now()));p.put("email",a.access.email());
  String scope=" where 1=1 ";
  if(a.access.hasRole("TEACHER"))scope+=" and exists(select 1 from batch_teacher_assignments ta join teachers t on t.id=ta.teacher_id join users u on u.id=t.user_id where ta.batch_id=e.batch_id and ta.active=true and u.email=:email)";
  if(a.student())scope+=" and e.status in ('PUBLISHED','COMPLETED') and exists(select 1 from batch_enrollments n join students s on s.id=n.student_id join users u on u.id=s.user_id where n.batch_id=e.batch_id and u.email=:email and n.enrollment_date<=e.eligibility_date and (n.withdrawal_date is null or n.withdrawal_date>e.eligibility_date) and (n.completion_date is null or n.completion_date>=e.eligibility_date))";
  if(batchId!=null){scope+=" and e.batch_id=:batch";p.put("batch",batchId);}
  String from=" from exams e join batches b on b.id=e.batch_id join courses c on c.id=b.course_id ";
  long total=count(from+scope,p),published=count(from+scope+" and e.status='PUBLISHED'",p),completed=count(from+scope+" and e.status='COMPLETED'",p),
   upcoming=count(from+scope+" and e.status='PUBLISHED' and e.end_date_time>:now",p),pending=count(from+scope+" and e.status='PUBLISHED' and e.end_date_time<=:now",p);
  String resultScope=scope+" and e.results_published=true ";
  if(a.student()){resultScope+=" and r.student_id=(select s.id from students s join users u on u.id=s.user_id where u.email=:email)";}
  else if(studentId!=null){resultScope+=" and r.student_id=:student";p.put("student",studentId);}
  String rf=from+" join exam_results r on r.exam_id=e.id ";
  var score=jdbc.queryForObject("select count(*),avg(100.0*r.obtained_marks/nullif(e.total_marks,0)),max(100.0*r.obtained_marks/nullif(e.total_marks,0)),min(100.0*r.obtained_marks/nullif(e.total_marks,0)),100.0*sum(case when r.obtained_marks>=e.passing_marks then 1 else 0 end)/nullif(count(*),0)"+rf+resultScope,p,
   (rs,n)->new ResultSummary(rs.getLong(1),scaled(rs.getBigDecimal(2)),scaled(rs.getBigDecimal(3)),scaled(rs.getBigDecimal(4)),scaled(rs.getBigDecimal(5))));
  var batches=metrics("select b.name,count(*),avg(100.0*r.obtained_marks/nullif(e.total_marks,0)),100.0*sum(case when r.obtained_marks>=e.passing_marks then 1 else 0 end)/count(*)"+rf+resultScope+" group by b.id,b.name order by b.name,b.id limit 100",p);
  var courses=metrics("select c.name,count(*),avg(100.0*r.obtained_marks/nullif(e.total_marks,0)),100.0*sum(case when r.obtained_marks>=e.passing_marks then 1 else 0 end)/count(*)"+rf+resultScope+" group by c.id,c.name order by c.name,c.id limit 100",p);
  String bucket="case when r.obtained_marks*100/e.total_marks>=90 then '90-100%' when r.obtained_marks*100/e.total_marks>=70 then '70-89%' when r.obtained_marks*100/e.total_marks>=50 then '50-69%' else 'Below 50%' end";
  var distribution=metrics("select concat(e.title,' · ',"+bucket+"),count(*),avg(100.0*r.obtained_marks/e.total_marks),100.0*sum(case when r.obtained_marks>=e.passing_marks then 1 else 0 end)/count(*)"+rf+resultScope+" group by 1 order by 1 limit 100",p);
  var recent=results.findAll((r,q,cb)->{
   jakarta.persistence.criteria.Join<ExamResult,Exam> ex=r.join("exam");var pred=cb.and(cb.isTrue(ex.get("resultsPublished")),a.scope(ex,q,cb));
   if(batchId!=null)pred=cb.and(pred,cb.equal(ex.get("batch").get("id"),batchId));
   if(a.student())pred=cb.and(pred,cb.equal(r.get("student").get("user").get("email"),a.access.email()));else if(studentId!=null)pred=cb.and(pred,cb.equal(r.get("student").get("id"),studentId));return pred;
  },PageRequest.of(0,100,Sort.by("evaluatedAt").descending().and(Sort.by("id")))).map(resultService::view).getContent();
  var next=exams.findAll((r,q,cb)->{var pred=cb.and(a.scope(r,q,cb),cb.equal(r.get("status"),ExamStatus.PUBLISHED),cb.greaterThan(r.get("endDateTime"),a.now()));if(batchId!=null)pred=cb.and(pred,cb.equal(r.get("batch").get("id"),batchId));return pred;},PageRequest.of(0,5,Sort.by("startDateTime"))).map(service::view).getContent();
  var needs=a.staff()?exams.findAll((r,q,cb)->{var pred=cb.and(a.scope(r,q,cb),cb.equal(r.get("status"),ExamStatus.PUBLISHED),cb.lessThanOrEqualTo(r.get("endDateTime"),a.now()));if(batchId!=null)pred=cb.and(pred,cb.equal(r.get("batch").get("id"),batchId));return pred;},PageRequest.of(0,5,Sort.by("endDateTime"))).map(service::view).getContent():List.<ExamView>of();
  return new Analytics(total,published,completed,upcoming,pending,score,batches,courses,distribution,recent,next,needs);
 }
 private long count(String sql,Map<String,Object> p){return jdbc.queryForObject("select count(*)"+sql,p,Long.class);}
 private static BigDecimal scaled(BigDecimal n){return n==null?null:n.setScale(2,RoundingMode.HALF_UP);}
 private List<Metric> metrics(String sql,Map<String,Object> p){return jdbc.query(sql,p,(rs,n)->new Metric(rs.getString(1),rs.getLong(2),scaled(rs.getBigDecimal(3)),scaled(rs.getBigDecimal(4))));}
}
