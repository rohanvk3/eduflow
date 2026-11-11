package com.eduflow.backend.service;

import com.eduflow.backend.dto.AnalyticsDtos.Data;
import com.eduflow.backend.dto.AnalyticsDtos.Filter;
import com.eduflow.backend.entity.Batch;
import com.eduflow.backend.entity.Course;
import com.eduflow.backend.entity.Student;
import com.eduflow.backend.entity.Teacher;
import com.eduflow.backend.repository.AnalyticsRepository;
import com.eduflow.backend.repository.AttendanceReportRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class AnalyticsService {
    final AnalyticsRepository db;
    final AcademicAccess auth;
    final ParentAccess parents;
    final AttendanceReportRepository attendance;
    final FinanceReportService finance;

    public AnalyticsService(AnalyticsRepository db, AcademicAccess auth, ParentAccess parents, AttendanceReportRepository attendance, FinanceReportService finance) {
        this.db = db;
        this.auth = auth;
        this.parents = parents;
        this.attendance = attendance;
        this.finance = finance;
    }

    public Filter filter(Filter f) {
        var d = AttendanceReportService.range(f.startDate(), f.endDate());
        for (Long id : Arrays.asList(f.courseId(), f.batchId(), f.teacherId(), f.studentId()))
            if (id != null && id < 1) throw new IllegalArgumentException("IDs must be positive");
        if (f.studentId() != null && db.find(Student.class, f.studentId()) == null)
            throw new com.eduflow.backend.exception.ResourceNotFoundException("Student not found");
        if (f.courseId() != null && db.find(Course.class, f.courseId()) == null)
            throw new com.eduflow.backend.exception.ResourceNotFoundException("Course not found");
        if (f.batchId() != null) {
            var b = db.find(Batch.class, f.batchId());
            if (b == null) throw new com.eduflow.backend.exception.ResourceNotFoundException("Batch not found");
            if (f.courseId() != null && !b.getCourse().getId().equals(f.courseId()))
                throw new IllegalArgumentException("Course/batch mismatch");
            if (auth.hasRole("TEACHER")) auth.readRoster(b.getId());
        }
        if (f.teacherId() != null) {
            var t = db.find(Teacher.class, f.teacherId());
            if (t == null) throw new com.eduflow.backend.exception.ResourceNotFoundException("Teacher not found");
            if (auth.hasRole("TEACHER") && (t.getUser() == null || !t.getUser().getEmail().equals(auth.email())))
                throw new AccessDeniedException("Own workload only");
        }
        if (f.examType() != null && !Set.of("ONLINE_MCQ", "OFFLINE").contains(f.examType()))
            throw new IllegalArgumentException("Invalid exam type");
        if (f.paymentStatus() != null && !Set.of("PAID", "OUTSTANDING", "OVERDUE").contains(f.paymentStatus()))
            throw new IllegalArgumentException("Invalid payment status");
        return new Filter(d[0], d[1], f.courseId(), f.batchId(), f.teacherId(), f.studentId(), f.paymentStatus(), f.examType());
    }

    Map<String, Object> args(Filter f) {
        var p = new HashMap<String, Object>();
        p.put("course", f.courseId());
        p.put("batch", f.batchId());
        p.put("student", f.studentId());
        p.put("teacher", f.teacherId());
        p.put("email", auth.email());
        p.put("from", f.startDate());
        p.put("to", f.endDate());
        p.put("start", f.startDate().atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant());
        p.put("end", f.endDate().plusDays(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant());
        p.put("localStart", f.startDate().atStartOfDay());
        p.put("localEnd", f.endDate().plusDays(1).atStartOfDay());
        p.put("today", BatchService.today());
        p.put("examType", f.examType());
        return p;
    }

    String scope(Filter f) {
        return (f.courseId() == null ? "" : " and b.course.id=:course") + (f.batchId() == null ? "" : " and b.id=:batch") + (f.teacherId() == null ? "" : " and exists(select t.id from BatchTeacherAssignment t where t.batch=b and t.active=true and t.teacher.id=:teacher)") + (auth.hasRole("TEACHER") ? " and exists(select t.id from BatchTeacherAssignment t where t.batch=b and t.active=true and t.teacher.user.email=:email)" : "");
    }

    String student(Filter f, String path) {
        return f.studentId() == null ? "" : " and " + path + "=:student";
    }

    static BigDecimal dec(Object x) {
        return x == null ? BigDecimal.ZERO : new BigDecimal(x.toString());
    }

    public static BigDecimal percent(Object n, Object d) {
        return dec(d).signum() == 0 ? null : dec(n).multiply(new BigDecimal("100")).divide(dec(d), 2, RoundingMode.HALF_UP);
    }

    Map<String, Object> map(Object... pairs) {
        var m = new LinkedHashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) m.put(pairs[i].toString(), pairs[i + 1]);
        return m;
    }

    Data data(String title, Filter f, Map<String, Object> m, List<Map<String, Object>> rows, int page, int size) {
        return new Data(title, f.startDate(), f.endDate(), m, rows.size() > size ? rows.subList(0, size) : rows, page, size, rows.size() > size);
    }

    void page(int page, int size) {
        CourseService.validatePage(page, size, "");
        if (page > 10000) throw new IllegalArgumentException("Page too large");
    }

    String active() {
        return " and n.status=com.eduflow.backend.entity.EnrollmentStatus.ACTIVE and n.enrollmentDate<=:today and (n.withdrawalDate is null or n.withdrawalDate>:today) and (n.completionDate is null or n.completionDate>=:today)";
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Data overview(Filter raw) {
        var f = filter(raw);
        var p = args(f);
        var m = map("activeStudents", db.scalar("select count(s) from Student s where s.status=com.eduflow.backend.entity.StudentStatus.ACTIVE" + (f.courseId() == null && f.batchId() == null ? "" : " and exists(select n.id from BatchEnrollment n join n.batch b where n.student=s" + active() + scope(f) + ")"), p), "activeTeachers", db.scalar("select count(t) from Teacher t where t.status=com.eduflow.backend.entity.StudentStatus.ACTIVE" + (f.courseId() == null && f.batchId() == null ? "" : " and exists(select ba.id from BatchTeacherAssignment ba join ba.batch b where ba.teacher=t and ba.active=true" + scope(f) + ")"), p), "activeCourses", db.scalar("select count(c) from Course c where c.status=com.eduflow.backend.entity.StudentStatus.ACTIVE" + (f.courseId() == null ? "" : " and c.id=:course") + (f.batchId() == null ? "" : " and exists(select b.id from Batch b where b.course=c and b.id=:batch)"), p), "activeBatches", db.scalar("select count(b) from Batch b where b.status=com.eduflow.backend.entity.BatchStatus.ACTIVE" + scope(f), p), "activeEnrollments", db.scalar("select count(n) from BatchEnrollment n join n.batch b where 1=1" + active() + scope(f), p));
        m.putAll(financial(f, "summary", 0, 10).metrics());
        m.putAll(academic(f, "summary", 0, 10).metrics());
        m.put("attendancePercentage", attendance.totals(af(f)).attendancePercentage());
        return data("Institute overview", f, m, List.of(), 0, 10);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Data financial(Filter raw, String section, int page, int size) {
        var f = filter(raw);
        page(page, size);
        if (f.paymentStatus() != null && !section.equals("outstanding"))
            throw new IllegalArgumentException("Payment status applies only to outstanding accounts");
        if (f.teacherId() != null || f.examType() != null)
            throw new IllegalArgumentException("Teacher/exam filters do not apply to finance");
        var r = finance.report(f.courseId(), f.batchId(), f.studentId(), f.startDate(), f.endDate(), 0, 1);
        var m = map("collections", r.totalCollections(), "agreedFees", r.totalAgreedFees(), "lifetimeCollections", r.lifetimeCollections(), "outstanding", r.outstandingBalance(), "overdue", r.overdueAmount(), "collectionRate", percent(r.lifetimeCollections(), r.totalAgreedFees()));
        var rows = new ArrayList<Map<String, Object>>();
        if (section.equals("outstanding")) {
            String paid = "coalesce((select sum(p.amount) from FeePayment p where p.feeAccount=a and p.status=com.eduflow.backend.entity.FeePaymentStatus.SUCCESS),0)";
            String due = "coalesce((select sum(i.amount) from FeeInstallment i where i.feeAccount=a and i.dueDate<:today),0)-coalesce((select sum(pa.amount) from PaymentAllocation pa where pa.installment.feeAccount=a and pa.installment.dueDate<:today and pa.payment.status=com.eduflow.backend.entity.FeePaymentStatus.SUCCESS),0)";
            String where = " from StudentFeeAccount a join a.enrollment n join n.batch b where 1=1" + scope(f) + student(f, "a.student.id");
            if (f.paymentStatus() != null)
                where += " and " + (f.paymentStatus().equals("PAID") ? "a.finalPayableAmount-" + paid + "=0" : f.paymentStatus().equals("OVERDUE") ? "(" + due + ")>0" : "a.finalPayableAmount-" + paid + ">0");
            if (f.paymentStatus() != null) {
                m.put("agreedFees", db.scalar("select coalesce(sum(a.finalPayableAmount),0)" + where, args(f)));
                m.put("lifetimeCollections", db.scalar("select coalesce(sum(" + paid + "),0)" + where, args(f)));
                m.put("outstanding", db.scalar("select coalesce(sum(a.finalPayableAmount-" + paid + "),0)" + where, args(f)));
                m.put("overdue", db.scalar("select coalesce(sum((" + due + ")),0)" + where, args(f)));
                m.remove("collections");
                m.put("collectionRate", percent(m.get("lifetimeCollections"), m.get("agreedFees")));
            }
            for (var x : db.rows("select a.id,a.student.fullName,b.name,a.finalPayableAmount," + paid + ",a.finalPayableAmount-" + paid + "," + due + where + " order by a.id", args(f), page * size, size + 1))
                rows.add(map("id", x[0], "label", x[1], "batch", x[2], "payable", x[3], "paid", x[4], "outstanding", x[5], "overdue", x[6]));
        } else if (section.equals("payments")) {
            String h = " from FeePayment p join p.feeAccount a join a.enrollment n join n.batch b where p.status=com.eduflow.backend.entity.FeePaymentStatus.SUCCESS and p.paymentDate between :from and :to" + scope(f) + student(f, "a.student.id");
            for (var x : db.rows("select p.id,p.receiptNumber,p.paymentDate,p.amount,p.paymentMethod,b.name" + h + " order by p.paymentDate,p.id", args(f), page * size, size + 1))
                rows.add(map("id", x[0], "label", x[1], "date", x[2], "amount", x[3], "method", x[4], "batch", x[5]));
        } else if (section.equals("batch-wise") || section.equals("course-wise")) {
            String group = section.equals("batch-wise") ? "b.id,b.name" : "b.course.id,b.course.name";
            String where = " from FeePayment p join p.feeAccount a join a.enrollment n join n.batch b where p.status=com.eduflow.backend.entity.FeePaymentStatus.SUCCESS and p.paymentDate between :from and :to" + scope(f) + student(f, "a.student.id");
            for (var x : db.rows("select " + group + ",sum(p.amount)" + where + " group by " + group + " order by 1", args(f), page * size, size + 1))
                rows.add(map("id", x[0], "label", x[1], "value", x[2]));
        } else {
            var series = switch (section) {
                case "payment-methods" -> r.paymentMethods();
                case "course-wise" -> r.courseCollections();
                case "batch-wise" -> r.batchOutstanding();
                default -> r.monthlyTrend();
            };
            for (var x : series) rows.add(map("label", x.label(), "value", x.amount()));
            return new Data("Finance (INR)", f.startDate(), f.endDate(), m, rows, 0, rows.size(), false);
        }
        return data("Finance (INR)", f, m, rows, page, size);
    }

    AttendanceReportRepository.Filter af(Filter f) {
        return new AttendanceReportRepository.Filter(f.courseId(), f.batchId(), f.studentId(), f.startDate(), f.endDate(), auth.hasRole("TEACHER") ? auth.email() : null);
    }

    public Data attendance(Filter raw, String section, int page, int size, BigDecimal threshold) {
        var f = filter(raw);
        page(page, size);
        if (f.teacherId() != null || f.examType() != null || f.paymentStatus() != null)
            throw new IllegalArgumentException("Unsupported attendance filter");
        if (threshold == null) threshold = new BigDecimal("75");
        if (threshold.signum() < 0 || threshold.compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("Threshold must be 0 to 100");
        var af = af(f);
        var t = attendance.totals(af);
        var m = map("attendancePercentage", t.attendancePercentage(), "present", t.presentCount(), "late", t.lateCount(), "absent", t.absentCount(), "excused", t.excusedCount(), "finalizedSessions", t.finalizedSessions());
        var rows = new ArrayList<Map<String, Object>>();
        if (section.equals("trend")) {
            var months = new TreeMap<String, long[]>();
            for (var x : attendance.trend(af)) {
                var counts = months.computeIfAbsent(x.date().toString().substring(0, 7), k -> new long[3]);
                counts[0] += x.totals().presentCount();
                counts[1] += x.totals().lateCount();
                counts[2] += x.totals().absentCount();
            }
            months.forEach((month, c) -> rows.add(map("label", month, "value", percent(c[0] + c[1], c[0] + c[1] + c[2]))));
            return new Data("Attendance", f.startDate(), f.endDate(), m, rows, 0, rows.size(), false);
        }
        var groups = attendance.groups(af, section.equals("student-wise"), page, size);
        for (var x : groups.content())
            rows.add(map("id", x.id(), "label", x.name(), "code", x.code(), "value", x.totals().attendancePercentage(), "belowThreshold", x.totals().attendancePercentage() != null && x.totals().attendancePercentage().compareTo(threshold) < 0, "present", x.totals().presentCount(), "late", x.totals().lateCount(), "absent", x.totals().absentCount(), "excused", x.totals().excusedCount()));
        return new Data("Attendance", f.startDate(), f.endDate(), m, rows, page, size, page + 1 < groups.totalPages());
    }

    String examWhere(Filter f) {
        return " from ExamResult r join r.exam e join e.batch b where e.resultsPublished=true and e.status in (com.eduflow.backend.entity.ExamStatus.PUBLISHED,com.eduflow.backend.entity.ExamStatus.COMPLETED) and e.totalMarks>0 and exists(select n.id from BatchEnrollment n where n.student=r.student and n.batch=b and n.enrollmentDate<=e.eligibilityDate and (n.withdrawalDate is null or n.withdrawalDate>e.eligibilityDate) and (n.completionDate is null or n.completionDate>=e.eligibilityDate)) and e.startDateTime>=:start and e.startDateTime<:end" + scope(f) + student(f, "r.student.id") + (f.examType() == null ? "" : " and str(e.examType)=:examType");
    }

    public Data academic(Filter raw, String section, int page, int size) {
        var f = filter(raw);
        page(page, size);
        if (f.paymentStatus() != null) throw new IllegalArgumentException("Payment filter is finance-only");
        var p = args(f);
        String where = examWhere(f);
        var total = db.scalar("select count(r)" + where, p);
        var avg = db.scalar("select avg(r.obtainedMarks*100.0/e.totalMarks)" + where, p);
        var passed = db.scalar("select sum(case when r.obtainedMarks>=e.passingMarks then 1 else 0 end)" + where, p);
        String aw = " from AssignmentRecipient ar join ar.assignment a join a.batch b where a.status<>com.eduflow.backend.entity.AssignmentStatus.DRAFT and a.publishedAt>=:start and a.publishedAt<:end" + scope(f) + student(f, "ar.student.id");
        var eligible = db.scalar("select count(ar)" + aw, p);
        var submitted = db.scalar("select count(ar)" + aw + " and exists(select s.id from AssignmentSubmission s where s.assignment=a and s.student=ar.student)", p);
        var m = map("examAverage", avg == null ? null : dec(avg).setScale(2, RoundingMode.HALF_UP), "publishedResults", total, "passPercentage", percent(passed, total), "assignmentEligible", eligible, "assignmentSubmitted", submitted, "assignmentCompletion", percent(submitted, eligible));
        var grading = " from AssignmentSubmission s join s.assignment a join a.batch b where s.status=com.eduflow.backend.entity.SubmissionStatus.GRADED and a.status<>com.eduflow.backend.entity.AssignmentStatus.DRAFT and a.publishedAt>=:start and a.publishedAt<:end" + scope(f) + student(f, "s.student.id");
        for (String bucket : List.of("below50", "50to79", "80plus")) {
            String condition = bucket.equals("below50") ? "s.obtainedMarks*100.0/a.maxMarks<50" : bucket.equals("50to79") ? "s.obtainedMarks*100.0/a.maxMarks>=50 and s.obtainedMarks*100.0/a.maxMarks<80" : "s.obtainedMarks*100.0/a.maxMarks>=80";
            m.put("assignmentMarks" + bucket, db.scalar("select count(s)" + grading + " and " + condition, p));
        }
        m.put("gradedSubmissions", db.scalar("select count(s)" + grading, p));
        var gradeAvg = db.scalar("select avg(s.obtainedMarks*100.0/a.maxMarks)" + grading, p);
        m.put("assignmentAverage", gradeAvg == null ? null : dec(gradeAvg).setScale(2, RoundingMode.HALF_UP));
        var rows = new ArrayList<Map<String, Object>>();
        if (section.equals("assignment-performance")) {
            for (var x : db.rows("select a.id,a.title,count(ar),sum(case when exists(select s.id from AssignmentSubmission s where s.assignment=a and s.student=ar.student) then 1 else 0 end)" + aw + " group by a.id,a.title order by a.id", p, page * size, size + 1))
                rows.add(map("id", x[0], "label", x[1], "eligible", x[2], "submitted", x[3], "value", percent(x[3], x[2])));
        } else {
            String group = switch (section) {
                case "course-wise" -> "b.course.id,b.course.name";
                case "batch-wise" -> "b.id,b.name";
                case "students" -> "r.student.id,r.student.fullName";
                default -> "e.id,e.title";
            };
            for (var x : db.rows("select " + group + ",avg(r.obtainedMarks*100.0/e.totalMarks),count(r),min(e.startDateTime)" + where + " group by " + group + " order by min(e.startDateTime),1", p, page * size, size + 1))
                rows.add(map("id", x[0], "label", x[1], "value", dec(x[2]).setScale(2, RoundingMode.HALF_UP), "results", x[3], "date", x[4]));
        }
        return data("Published academic performance", f, m, rows, page, size);
    }

    public Data batches(Filter raw, int page, int size) {
        return batches(raw, page, size, "name,asc");
    }

    public Data batches(Filter raw, int page, int size, String sort) {
        if (!Set.of("name,asc", "name,desc").contains(sort))
            throw new IllegalArgumentException("Batch sort must be name,asc or name,desc");
        var f = filter(raw);
        page(page, size);
        var rows = new ArrayList<Map<String, Object>>();
        for (var x : db.rows("select b.id,b.name,b.course.name from Batch b where 1=1" + scope(f) + " order by b.name " + (sort.endsWith("desc") ? "desc" : "asc") + ",b.id", args(f), page * size, size + 1)) {
            var one = new Filter(f.startDate(), f.endDate(), f.courseId(), ((Number) x[0]).longValue(), null, f.studentId(), null, f.examType());
            var m = map("id", x[0], "label", x[1], "course", x[2], "activeStudents", db.scalar("select count(distinct n.student.id) from BatchEnrollment n join n.batch b where n.student.status=com.eduflow.backend.entity.StudentStatus.ACTIVE" + active() + scope(one), args(one)), "attendance", attendance.totals(af(one)).attendancePercentage());
            m.put("teachers", String.join(", ", db.rows("select t.teacher.id,t.teacher.fullName from BatchTeacherAssignment t where t.active=true and t.batch.id=:id", Map.of("id", x[0]), 0, 100).stream().map(t -> t[1].toString()).toList()));
            m.putAll(academic(one, "summary", 0, 1).metrics());
            if (auth.hasRole("ADMIN"))
                m.putAll(financial(new Filter(one.startDate(), one.endDate(), one.courseId(), one.batchId(), null, one.studentId(), null, null), "summary", 0, 1).metrics());
            rows.add(m);
        }
        return data("Batch performance", f, Map.of(), rows, page, size);
    }

    public Data teachers(Filter raw, int page, int size) {
        if(raw.studentId()!=null||raw.examType()!=null||raw.paymentStatus()!=null)throw new IllegalArgumentException("Unsupported workload filter");
        var f = filter(raw);
        page(page, size);
        var p = args(f);
        String wh = " where 1=1" + (f.teacherId() == null ? "" : " and t.id=:teacher") + (auth.hasRole("TEACHER") ? " and t.user.email=:email" : "");
        var rows = new ArrayList<Map<String, Object>>();
        for (var x : db.rows("select t.id,t.fullName from Teacher t" + wh + " order by t.id", p, page * size, size + 1)) {
            p.put("tid", x[0]);
            String bs = scope(new Filter(f.startDate(), f.endDate(), f.courseId(), f.batchId(), null, null, null, null));
            var m = map("id", x[0], "label", x[1]);
            m.put("assignedBatches", db.scalar("select count(t) from BatchTeacherAssignment t join t.batch b where t.active=true and t.teacher.id=:tid" + bs, p));
            m.put("assignedCourses", db.scalar("select count(distinct b.course.id) from BatchTeacherAssignment t join t.batch b where t.active=true and t.teacher.id=:tid" + bs, p));
            m.put("weeklyScheduleSlots", db.scalar("select count(s) from BatchSchedule s join s.batch b where s.active=true and s.teacher.id=:tid" + bs, p));
            for (String kind : List.of("attendance", "assignments", "graded", "exams")) {
                String hql = switch (kind) {
                    case "attendance" ->
                            "select count(s) from AttendanceSession s join s.batch b where s.markedBy.id=(select t.user.id from Teacher t where t.id=:tid) and s.status=com.eduflow.backend.entity.AttendanceSessionStatus.FINALIZED and s.sessionDate between :from and :to";
                    case "assignments" ->
                            "select count(a) from Assignment a join a.batch b where a.createdBy.id=(select t.user.id from Teacher t where t.id=:tid) and a.createdAt>=:localStart and a.createdAt<:localEnd";
                    case "graded" ->
                            "select count(s) from AssignmentSubmission s join s.assignment a join a.batch b where s.gradedBy.id=(select t.user.id from Teacher t where t.id=:tid) and s.gradedAt>=:start and s.gradedAt<:end";
                    default ->
                            "select count(e) from Exam e join e.batch b where e.createdBy.id=(select t.user.id from Teacher t where t.id=:tid) and e.startDateTime>=:start and e.startDateTime<:end";
                };
                m.put(kind, db.scalar(hql + bs, p));
            }
            rows.add(m);
        }
        return data("Teacher workload", f, Map.of(), rows, page, size);
    }

    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> progress(Long id, Filter raw) {
        if (auth.hasRole("PARENT")) parents.child(id);
        else if (auth.hasRole("STUDENT")) {
            var s = db.find(Student.class, id);
            if (s == null || s.getUser() == null || !s.getUser().getEmail().equals(auth.email()))
                throw new AccessDeniedException("Own progress only");
        } else if (!auth.hasRole("ADMIN") && !auth.hasRole("TEACHER"))
            throw new AccessDeniedException("Progress denied");
        var f = filter(new Filter(raw.startDate(), raw.endDate(), raw.courseId(), raw.batchId(), null, id, null, raw.examType()));
        if (auth.hasRole("TEACHER") && dec(db.scalar("select count(n) from BatchEnrollment n join n.batch b where n.student.id=:student" + scope(f), args(f))).signum() == 0)
            throw new AccessDeniedException("Student outside assigned batches");
        return map("attendance", attendance(f, "trend", 0, 100, null), "academics", academic(f, "exam-trend", 0, 100), "assignments", academic(f, "assignment-performance", 0, 100), "courses", academic(f, "course-wise", 0, 100), "batches", attendance(f, "batch-wise", 0, 100, null));
    }
}
