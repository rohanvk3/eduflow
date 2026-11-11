package com.eduflow.backend.service;
import com.eduflow.backend.dto.*;
import com.eduflow.backend.dto.ParentDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.repository.*;
import jakarta.persistence.criteria.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true) @PreAuthorize("hasRole('PARENT')")
public class ParentPortalService {
 final ParentAccess access;final AssignmentAccess a;final BatchEnrollmentRepository enrollments;final AttendanceReportRepository attendance;
 final StudentFeeAccountRepository accounts;final FinanceService finance;final ExamRepository exams;final ExamResultRepository results;final AssignmentRepository assignments;final AssignmentSubmissionRepository submissions;final StudyMaterialRepository materials;
 public ParentPortalService(ParentAccess access,AssignmentAccess a,BatchEnrollmentRepository enrollments,AttendanceReportRepository attendance,StudentFeeAccountRepository accounts,FinanceService finance,ExamRepository exams,ExamResultRepository results,AssignmentRepository assignments,AssignmentSubmissionRepository submissions,StudyMaterialRepository materials){this.access=access;this.a=a;this.enrollments=enrollments;this.attendance=attendance;this.accounts=accounts;this.finance=finance;this.exams=exams;this.results=results;this.assignments=assignments;this.submissions=submissions;this.materials=materials;}
 public PageResponse<ChildView> children(int page,int size,String search){
  CourseService.validatePage(page,size,search);var parent=access.profile();if(parent.isEmpty()||parent.get().getStatus()!=StudentStatus.ACTIVE)return new PageResponse<>(List.of(),page,size,0,0);
  String pattern=CourseService.pattern(search);return PageResponse.from(access.links.findAll((r,q,cb)->cb.and(cb.equal(r.get("parent").get("id"),parent.get().getId()),cb.equal(r.get("status"),ParentLinkStatus.VERIFIED),cb.or(cb.like(cb.lower(r.get("student").get("fullName")),pattern,'\\'),cb.like(cb.lower(r.get("student").get("admissionNumber")),pattern,'\\'))),FinanceService.paging(page,size,"id,asc",Set.of("id"))).map(this::childView));
 }
 public ChildView child(Long studentId){return childView(access.relationship(studentId));}
 private ChildView childView(ParentStudentLink l){
  var s=l.getStudent();var today=a.now().atZone(AssignmentAccess.ZONE).toLocalDate();
  var rows=enrollments.findAll((r,q,cb)->cb.equal(r.get("student").get("id"),s.getId()),Sort.by(Sort.Direction.DESC,"enrollmentDate","id")).stream().map(n->{
   var b=n.getBatch();boolean active=n.getStatus()==EnrollmentStatus.ACTIVE&&!n.getEnrollmentDate().isAfter(today)&&(n.getWithdrawalDate()==null||n.getWithdrawalDate().isAfter(today))&&(n.getCompletionDate()==null||!n.getCompletionDate().isBefore(today));
   return new EnrollmentView(n.getId(),b.getCourse().getId(),b.getCourse().getName(),b.getId(),b.getName(),b.getStatus(),n.getStatus(),n.getEnrollmentDate(),n.getWithdrawalDate(),n.getCompletionDate(),active&&b.getStatus()==BatchStatus.ACTIVE);
  }).toList();
  return new ChildView(s.getId(),s.getFullName(),s.getAdmissionNumber(),s.getStatus(),l.getRelationshipType(),l.getPrimaryGuardian(),rows);
 }
 public AttendanceDtos.Personal attendance(Long sid,Long batchId,LocalDate from,LocalDate to,int page,int size,int batchPage){
  access.child(sid);CourseService.validatePage(page,size,"");CourseService.validatePage(batchPage,size,"");if(page>100000||batchPage>100000)throw new IllegalArgumentException("Page too large");var dates=AttendanceReportService.range(from,to);
  var f=new AttendanceReportRepository.Filter(null,batchId,sid,dates[0],dates[1],null);
  // The existing report repository counts only finalized records accepted by enrollment validation.
  var source=attendance.history(f,page,size);
  var safeHistory=new PageResponse<AttendanceDtos.History>(source.content().stream().map(h->new AttendanceDtos.History(h.sessionId(),h.batchId(),h.batchName(),h.courseName(),h.sessionDate(),h.startTime(),h.endTime(),h.topic(),h.status(),null)).toList(),source.page(),source.size(),source.totalElements(),source.totalPages());
  return new AttendanceDtos.Personal(true,attendance.totals(f),attendance.groups(f,false,batchPage,size),attendance.trend(f),safeHistory,dates[0],dates[1]);
 }
 private BigDecimal amount(String query,Long sid){var n=(Number)a.em.createQuery(query).setParameter("sid",sid).getSingleResult();return n==null?new BigDecimal("0.00"):new BigDecimal(n.toString()).setScale(2,RoundingMode.UNNECESSARY);}
 private FeeTotals feeTotals(Long sid){
  BigDecimal payable=amount("select sum(a.finalPayableAmount) from StudentFeeAccount a where a.student.id=:sid",sid);
  BigDecimal paid=amount("select sum(p.amount) from FeePayment p where p.feeAccount.student.id=:sid and p.status=com.eduflow.backend.entity.FeePaymentStatus.SUCCESS",sid);
  var due=a.em.createQuery("select sum(i.amount) from FeeInstallment i where i.feeAccount.student.id=:sid and i.dueDate<:today").setParameter("sid",sid).setParameter("today",BatchService.today()).getSingleResult();
  var allocation=a.em.createQuery("select sum(p.amount) from PaymentAllocation p where p.installment.feeAccount.student.id=:sid and p.installment.dueDate<:today and p.payment.status=com.eduflow.backend.entity.FeePaymentStatus.SUCCESS").setParameter("sid",sid).setParameter("today",BatchService.today()).getSingleResult();
  BigDecimal overdue=(due==null?BigDecimal.ZERO:(BigDecimal)due).subtract(allocation==null?BigDecimal.ZERO:(BigDecimal)allocation).setScale(2);
  return new FeeTotals(payable,paid,payable.subtract(paid),overdue);
 }
 public Fees fees(Long sid,int page,int size){
  access.child(sid);
  return new Fees(feeTotals(sid),PageResponse.from(accounts.findAll((r,q,cb)->cb.equal(r.get("student").get("id"),sid),FinanceService.paging(page,size,"id,desc",Set.of("id"))).map(account->{var d=finance.parentAccount(sid,account.getId());var f=d.account();
   return new FeeAccountView(f.id(),f.courseName(),f.batchName(),f.finalPayableAmount(),f.paidAmount(),f.outstandingAmount(),f.overdueAmount(),d.installments(),d.payments().stream().map(p->new PaymentView(p.id(),p.receiptNumber(),p.amount(),p.paymentDate(),p.paymentMethod(),p.status())).toList());
  })));
 }
 private Predicate eligible(From<?,?> e,CriteriaQuery<?> q,CriteriaBuilder cb,Long sid){
  var sub=q.subquery(Long.class);var n=sub.from(BatchEnrollment.class);sub.select(n.get("id")).where(cb.equal(n.get("student").get("id"),sid),cb.equal(n.get("batch").get("id"),e.get("batch").get("id")),cb.lessThanOrEqualTo(n.get("enrollmentDate"),e.get("eligibilityDate")),cb.or(cb.isNull(n.get("withdrawalDate")),cb.greaterThan(n.get("withdrawalDate"),e.get("eligibilityDate"))),cb.or(cb.isNull(n.get("completionDate")),cb.greaterThanOrEqualTo(n.get("completionDate"),e.get("eligibilityDate"))));return cb.exists(sub);
 }
 private Specification<ExamResult> resultFilter(Long sid,Long courseId,Long batchId,LocalDate[] dates){return (r,q,cb)->{var e=r.join("exam");var p=cb.and(cb.equal(r.get("student").get("id"),sid),cb.isTrue(e.get("resultsPublished")),e.get("status").in(ExamStatus.PUBLISHED,ExamStatus.COMPLETED),eligible(e,q,cb,sid),cb.greaterThanOrEqualTo(e.get("startDateTime"),start(dates[0])),cb.lessThan(e.get("startDateTime"),start(dates[1].plusDays(1))));if(courseId!=null)p=cb.and(p,cb.equal(e.get("batch").get("course").get("id"),courseId));if(batchId!=null)p=cb.and(p,cb.equal(e.get("batch").get("id"),batchId));return p;};}
 public PageResponse<ResultView> results(Long sid,Long courseId,Long batchId,LocalDate from,LocalDate to,int page,int size){
  access.child(sid);var dates=AttendanceReportService.range(from,to);return PageResponse.from(results.findAll(resultFilter(sid,courseId,batchId,dates),FinanceService.paging(page,size,"evaluatedAt,desc",Set.of("evaluatedAt"))).map(this::resultView));
 }
 private ResultView resultView(ExamResult r){var e=r.getExam();var b=e.getBatch();var percentage=ExamResultService.percent(r.getObtainedMarks(),e.getTotalMarks());return new ResultView(r.getId(),e.getId(),e.getTitle(),b.getCourse().getId(),b.getCourse().getName(),b.getId(),b.getName(),e.getStartDateTime(),r.getObtainedMarks(),e.getTotalMarks(),percentage,ExamResultService.grade(percentage),r.getObtainedMarks().compareTo(e.getPassingMarks())>=0);}
 private Specification<Assignment> assignmentFilter(Long sid,String search,Long batchId,String status,LocalDate[] dates){
  if(status!=null&&!Set.of("PENDING","SUBMITTED","LATE","GRADED").contains(status))throw new IllegalArgumentException("Invalid submission filter");String pattern=CourseService.pattern(search);
  return (r,q,cb)->{
   var recipient=q.subquery(Long.class);var ar=recipient.from(AssignmentRecipient.class);recipient.select(ar.get("id")).where(cb.equal(ar.get("assignment").get("id"),r.get("id")),cb.equal(ar.get("student").get("id"),sid));
   var work=q.subquery(Long.class);var w=work.from(AssignmentSubmission.class);work.select(w.get("id")).where(cb.equal(w.get("assignment").get("id"),r.get("id")),cb.equal(w.get("student").get("id"),sid));
   var p=cb.and(cb.exists(recipient),cb.or(cb.equal(r.get("status"),AssignmentStatus.PUBLISHED),cb.and(cb.equal(r.get("status"),AssignmentStatus.CLOSED),cb.exists(work))),cb.like(cb.lower(r.get("title")),pattern,'\\'));
   if(batchId!=null)p=cb.and(p,cb.equal(r.get("batch").get("id"),batchId));
   if(dates!=null)p=cb.and(p,cb.greaterThanOrEqualTo(r.get("dueDateTime"),start(dates[0])),cb.lessThan(r.get("dueDateTime"),start(dates[1].plusDays(1))));
   if("PENDING".equals(status))p=cb.and(p,cb.not(cb.exists(work)));
   else if(status!=null){var match=q.subquery(Long.class);var s=match.from(AssignmentSubmission.class);var condition="LATE".equals(status)?cb.isTrue(s.get("late")):cb.equal(s.get("status"),SubmissionStatus.valueOf(status));match.select(s.get("id")).where(cb.equal(s.get("assignment").get("id"),r.get("id")),cb.equal(s.get("student").get("id"),sid),condition);p=cb.and(p,cb.exists(match));}
   return p;
  };
 }
 public PageResponse<AssignmentView> assignments(Long sid,String search,Long batchId,String status,LocalDate from,LocalDate to,int page,int size){
  access.child(sid);CourseService.validatePage(page,size,search);LocalDate[] dates=from==null&&to==null?null:AttendanceReportService.range(from,to);
  return PageResponse.from(assignments.findAll(assignmentFilter(sid,search,batchId,status,dates),FinanceService.paging(page,size,"dueDateTime,desc",Set.of("dueDateTime"))).map(x->assignmentView(x,sid)));
 }
 private AssignmentView assignmentView(Assignment x,Long sid){var b=x.getBatch();var s=submissions.findByAssignmentIdAndStudentId(x.getId(),sid).orElse(null);boolean graded=s!=null&&s.getStatus()==SubmissionStatus.GRADED;return new AssignmentView(x.getId(),x.getTitle(),b.getCourse().getId(),b.getCourse().getName(),b.getId(),b.getName(),x.getDueDateTime(),x.getStatus(),s==null?"PENDING":s.getStatus().name(),s==null?null:s.getSubmittedAt(),s!=null&&s.getLate(),x.getMaxMarks(),graded?s.getObtainedMarks():null,graded?s.getFeedback():null,graded?s.getGradedAt():null);}
 public PageResponse<ResourceView> resources(Long sid,String search,Long courseId,Long batchId,int page,int size){
  access.child(sid);CourseService.validatePage(page,size,search);String pattern=CourseService.pattern(search);LocalDate day=a.now().atZone(AssignmentAccess.ZONE).toLocalDate();
  return PageResponse.from(materials.findAll((r,q,cb)->{
   var sub=q.subquery(Long.class);var n=sub.from(BatchEnrollment.class);sub.select(n.get("id")).where(cb.equal(n.get("student").get("id"),sid),cb.equal(n.get("status"),EnrollmentStatus.ACTIVE),cb.lessThanOrEqualTo(n.get("enrollmentDate"),day),cb.or(cb.isNull(n.get("withdrawalDate")),cb.greaterThan(n.get("withdrawalDate"),day)),cb.or(cb.isNull(n.get("completionDate")),cb.greaterThanOrEqualTo(n.get("completionDate"),day)),cb.equal(n.get("batch").get("course").get("id"),r.get("course").get("id")),cb.or(cb.isNull(r.get("batch")),cb.equal(n.get("batch").get("id"),r.get("batch").get("id"))));
   var p=cb.and(cb.exists(sub),cb.equal(r.get("visibility"),ResourceVisibility.PUBLISHED),cb.like(cb.lower(r.get("title")),pattern,'\\'));if(courseId!=null)p=cb.and(p,cb.equal(r.get("course").get("id"),courseId));if(batchId!=null)p=cb.and(p,cb.equal(r.get("batch").get("id"),batchId));return p;
  },FinanceService.paging(page,size,"publishedAt,desc",Set.of("publishedAt"))).map(m->new ResourceView(m.getId(),m.getTitle(),m.getDescription(),m.getCourse().getName(),m.getBatch()==null?null:m.getBatch().getName(),m.getCategory().getName(),m.getResourceType(),m.getPublishedAt())));
 }
 private Instant start(LocalDate day){return day.atStartOfDay(AssignmentAccess.ZONE).toInstant();}
 private BigDecimal average(Specification<ExamResult> filter){
  var cb=a.em.getCriteriaBuilder();var q=cb.createQuery(Double.class);var r=q.from(ExamResult.class);q.select(cb.avg(cb.prod(cb.quot(r.get("obtainedMarks"),r.get("exam").get("totalMarks")),100))).where(filter.toPredicate(r,q,cb));
  Double value=a.em.createQuery(q).getSingleResult();return value==null?null:BigDecimal.valueOf(value).setScale(2,RoundingMode.HALF_UP);
 }
 private long submitted(Specification<Assignment> f,Long sid){return assignments.count(f.and((r,q,cb)->{var sub=q.subquery(Long.class);var s=sub.from(AssignmentSubmission.class);sub.select(s.get("id")).where(cb.equal(s.get("assignment").get("id"),r.get("id")),cb.equal(s.get("student").get("id"),sid));return cb.exists(sub);}));}
 public Progress progress(Long sid,LocalDate from,LocalDate to){
  access.child(sid);var dates=AttendanceReportService.range(from,to);var f=new AttendanceReportRepository.Filter(null,null,sid,dates[0],dates[1],null);var af=assignmentFilter(sid,"",null,null,dates);long eligible=assignments.count(af),submitted=submitted(af,sid);
  var rf=resultFilter(sid,null,null,dates);var examTrend=results.findAll(rf,PageRequest.of(0,100,Sort.by("exam.startDateTime","id"))).map(this::resultView).getContent();
  var assignmentTrend=assignments.findAll(af,PageRequest.of(0,100,Sort.by("dueDateTime","id"))).map(x->assignmentView(x,sid)).getContent();
  var courses=new LinkedHashMap<Long,String>();child(sid).enrollments().forEach(n->courses.put(n.courseId(),n.courseName()));
  var courseProgress=new ArrayList<CourseProgress>();for(var c:courses.entrySet()){var cf=resultFilter(sid,c.getKey(),null,dates);var ca=af.and((r,q,cb)->cb.equal(r.get("batch").get("course").get("id"),c.getKey()));long total=assignments.count(ca),done=submitted(ca,sid);courseProgress.add(new CourseProgress(c.getKey(),c.getValue(),results.count(cf),average(cf),total,done,AssignmentAccess.percent(done,total)));}
  return new Progress(dates[0],dates[1],attendance.totals(f),attendance.trend(f),eligible,submitted,AssignmentAccess.percent(submitted,eligible),average(rf),examTrend,assignmentTrend,courseProgress);
 }
 public Dashboard dashboard(Long sid,LocalDate from,LocalDate to){
  if(sid==null){var children=children(0,1,"").content();if(children.isEmpty())return new Dashboard(false,null,null,null,0,0,List.of(),List.of(),List.of(),List.of(),List.of(),null);sid=children.getFirst().id();}
  var child=child(sid);final Long id=sid;Instant now=a.now();
  Specification<Exam> upcoming=(r,q,cb)->cb.and(cb.equal(r.get("status"),ExamStatus.PUBLISHED),cb.greaterThan(r.get("endDateTime"),now),eligible(r,q,cb,id));
  long count=exams.count(upcoming);var upcomingRows=exams.findAll(upcoming,PageRequest.of(0,6,Sort.by("startDateTime","id"))).map(e->new UpcomingExam(e.getId(),e.getTitle(),e.getBatch().getCourse().getName(),e.getBatch().getName(),e.getStartDateTime(),e.getEndDateTime())).getContent();
  var af=assignmentFilter(id,"",null,"PENDING",null).and((r,q,cb)->cb.equal(r.get("status"),AssignmentStatus.PUBLISHED));
  long pending=assignments.count(af);var deadlines=assignments.findAll(af,PageRequest.of(0,6,Sort.by("dueDateTime","id"))).map(x->assignmentView(x,id)).getContent();
  var fees=fees(id,0,5);var progress=progress(id,from,to);var recent=results(id,null,null,from,to,0,5).content();
  var events=new ArrayList<Activity>();for(var r:recent){var result=a.find(ExamResult.class,r.id(),"Result",false);events.add(new Activity("RESULT","Published result: "+r.examTitle(),result.getExam().getResultsPublishedAt()==null?result.getEvaluatedAt():result.getExam().getResultsPublishedAt()));}
  for(var s:progress.assignmentTrend())if(s.gradedAt()!=null)events.add(new Activity("ASSIGNMENT","Feedback received: "+s.title(),s.gradedAt()));else if(s.submittedAt()!=null)events.add(new Activity("SUBMISSION","Work submitted: "+s.title(),s.submittedAt()));
  var history=attendance(id,null,from,to,0,5,0).history().content();for(var r:history)events.add(new Activity("ATTENDANCE",r.batchName()+": "+r.status(),r.sessionDate().atStartOfDay(AssignmentAccess.ZONE).toInstant()));
  events.sort(Comparator.comparing(Activity::occurredAt,Comparator.nullsLast(Comparator.reverseOrder())));
  return new Dashboard(true,child,progress.attendance(),fees.totals(),count,pending,upcomingRows,recent,deadlines,fees.accounts().content(),events.stream().limit(10).toList(),progress);
 }
}
