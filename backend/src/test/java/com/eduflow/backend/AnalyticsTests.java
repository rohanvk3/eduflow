package com.eduflow.backend;

import com.eduflow.backend.entity.*;
import com.eduflow.backend.service.FinanceReportService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalyticsTests extends NotificationManagementTests {
    @Test
    void ledgerReconciliationAndDateScope() throws Exception {
        var i = installment(today.minusDays(1));
        for (int n = 0; n < 2; n++) {
            var p = new FeePayment();
            p.setFeeAccount(i.getFeeAccount());
            p.setAmount(new BigDecimal(n == 0 ? "40" : "60"));
            p.setPaymentDate(today);
            p.setPaymentMethod(PaymentMethod.CASH);
            p.setReceiptNumber("RECON-" + n);
            p.setIdempotencyKey("RECON-" + n);
            p.setRequestHash("hash");
            p.setRecordedBy(adminUser);
            p.setStatus(n == 0 ? FeePaymentStatus.SUCCESS : FeePaymentStatus.VOIDED);
            persist(p);
            var a = new PaymentAllocation();
            a.setPayment(p);
            a.setInstallment(i);
            a.setAmount(p.getAmount());
            persist(a);
        }
        mvc.perform(get("/api/analytics/finance/summary").header("Authorization", "Bearer " + admin)).andExpect(status().isOk()).andExpect(jsonPath("metrics.collections").value(40)).andExpect(jsonPath("metrics.outstanding").value(960)).andExpect(jsonPath("metrics.overdue").value(60));
        mvc.perform(get("/api/analytics/finance/summary").param("startDate", today.minusDays(2).toString()).param("endDate", today.minusDays(1).toString()).header("Authorization", "Bearer " + admin)).andExpect(jsonPath("metrics.collections").value(0)).andExpect(jsonPath("metrics.outstanding").value(960));
        mvc.perform(get("/api/analytics/finance/outstanding").param("paymentStatus", "OVERDUE").header("Authorization", "Bearer " + admin)).andExpect(status().isOk()).andExpect(jsonPath("rows[0].overdue").value(60));
    }

    @Test
    void finalizedAttendanceAndAssignmentCompletion() throws Exception {
        int n = 0;
        for (var status : AttendanceStatus.values()) {
            var s = new AttendanceSession();
            s.setBatch(batch);
            s.setSessionDate(today);
            s.setOccurrenceKey("QA" + n++);
            s.setMarkedBy(adminUser);
            s.setStatus(AttendanceSessionStatus.FINALIZED);
            persist(s);
            var r = new AttendanceRecord();
            r.setSession(s);
            r.setStudent(s1);
            r.setStatus(status);
            persist(r);
        }
        mvc.perform(get("/api/analytics/attendance/summary").header("Authorization", "Bearer " + admin)).andExpect(status().isOk()).andExpect(jsonPath("metrics.attendancePercentage").value(66.67)).andExpect(jsonPath("metrics.excused").value(1));
        var a = assignment();
        var sub = new AssignmentSubmission();
        sub.setAssignment(a);
        sub.setStudent(s1);
        sub.setSubmittedAt(time.get());
        sub.setSubmissionText("Answer");
        persist(sub);
        mvc.perform(get("/api/analytics/academics/summary").header("Authorization", "Bearer " + admin)).andExpect(jsonPath("metrics.assignmentCompletion").value(100));
    }

    @Test
    void allAnalyticsQueriesAndExportFormatsExecute() throws Exception {
        installment(today.minusDays(2));
        var e = exam(time.get());
        e.setResultsPublished(true);
        var r = new ExamResult();
        r.setExam(e);
        r.setStudent(s1);
        r.setObtainedMarks(new BigDecimal("80"));
        r.setEvaluatedAt(time.get());
        persist(r);
        assignment();
        for (String path : List.of("overview", "finance/summary", "finance/outstanding", "finance/payments", "finance/course-wise", "attendance/summary", "attendance/trend", "attendance/student-wise", "academics/summary", "academics/exam-trend", "academics/course-wise", "academics/assignment-performance", "batches", "teachers", "students/" + s1.getId() + "/progress"))
            mvc.perform(get("/api/analytics/" + path).header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
        for (String type : List.of("REVENUE", "OUTSTANDING_FEES", "PAYMENT_COLLECTIONS", "ATTENDANCE", "STUDENT_PERFORMANCE", "EXAM_RESULTS", "ASSIGNMENT_COMPLETION", "BATCH_PERFORMANCE", "TEACHER_WORKLOAD")) {
            mvc.perform(get("/api/reports").param("reportType", type).header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
            var csv = mvc.perform(get("/api/reports/export/csv").param("reportType", type).header("Authorization", "Bearer " + admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(csv).contains("Report");
        }
        byte[] pdf = mvc.perform(get("/api/reports/export/pdf").param("reportType", "EXAM_RESULTS").header("Authorization", "Bearer " + admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1)).startsWith("%PDF");
    }

    @Test
    void normalizedPublishedScoresAndNoData() throws Exception {
        mvc.perform(get("/api/analytics/academics/summary").header("Authorization", "Bearer " + admin)).andExpect(jsonPath("metrics.examAverage").isEmpty());
        for (int i = 1; i <= 2; i++) {
            var e = exam(time.get());
            e.setTotalMarks(new BigDecimal(i == 1 ? "100" : "50"));
            e.setResultsPublished(true);
            var r = new ExamResult();
            r.setExam(e);
            r.setStudent(s1);
            r.setObtainedMarks(new BigDecimal(i == 1 ? "80" : "20"));
            r.setEvaluatedAt(time.get());
            persist(r);
        }
        exam(time.get());
        mvc.perform(get("/api/analytics/academics/summary").header("Authorization", "Bearer " + admin)).andExpect(jsonPath("metrics.examAverage").value(60)).andExpect(jsonPath("metrics.publishedResults").value(2));
    }

    @Test
    void permissionsAndFiltersAreEnforced() throws Exception {
        mvc.perform(get("/api/analytics/overview")).andExpect(status().isUnauthorized());
        for (String t : List.of(student, parent, teacher))
            mvc.perform(get("/api/analytics/finance/summary").header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
        mvc.perform(get("/api/analytics/attendance/summary").param("batchId", otherBatch.getId().toString()).header("Authorization", "Bearer " + teacher)).andExpect(status().isForbidden());
        mvc.perform(get("/api/analytics/students/" + s2.getId() + "/progress").header("Authorization", "Bearer " + student)).andExpect(status().isForbidden());
        mvc.perform(get("/api/analytics/students/" + s1.getId() + "/progress").header("Authorization", "Bearer " + student)).andExpect(status().isOk());
        mvc.perform(get("/api/analytics/students/" + s1.getId() + "/progress").header("Authorization", "Bearer " + parent)).andExpect(status().isOk());
        mvc.perform(get("/api/reports/export/csv").param("reportType", "REVENUE").header("Authorization", "Bearer " + teacher)).andExpect(status().isForbidden());
        mvc.perform(get("/api/analytics/attendance/summary").param("startDate", "2026-12-01").param("endDate", "2026-01-01").header("Authorization", "Bearer " + admin)).andExpect(status().isBadRequest());
        assertThat(FinanceReportService.csvCell("=HYPERLINK(1)")).startsWith("\"'");
    }
}
