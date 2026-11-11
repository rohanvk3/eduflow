package com.eduflow.backend;

import com.eduflow.backend.entity.FeeInstallment;
import com.eduflow.backend.entity.Notification;
import com.eduflow.backend.entity.NotificationType;
import com.eduflow.backend.repository.NotificationJobRepository;
import com.eduflow.backend.repository.NotificationRepository;
import com.eduflow.backend.service.NotificationDeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:notification-events;MODE=MySQL;DB_CLOSE_DELAY=-1", "app.exams.expiry-enabled=false"})
class NotificationAutomaticEventTests extends ParentPortalTests {
    @Autowired
    NotificationJobRepository jobs;
    @Autowired
    NotificationRepository notifications;
    @Autowired
    NotificationDeliveryService delivery;

    void ready() throws Exception {
        student.setUser(users.findByEmail("student@parent.test").orElseThrow());
        em.flush();
        verified();
    }

    void drain() {
        for (int i = 0; i < 50; i++) if (delivery.deliverBatch() == 0) return;
        throw new AssertionError("Delivery did not complete");
    }

    @Test
    void publishedResultsNotifyStudentsAndVerifiedParentsWithoutMarks() throws Exception {
        ready();
        var r = result(false);
        long id = r.getExam().getId();
        mvc.perform(json(post("/api/exams/" + id + "/results/publish"), Map.of("version", r.getExam().getVersion()), admin)).andExpect(status().isOk());
        drain();
        assertThat(notifications.findAll().stream().filter(n -> n.getType() == NotificationType.EXAM_RESULT_PUBLISHED).count()).isEqualTo(2);
        assertThat(notifications.findAll().stream().map(Notification::getMessage)).allSatisfy(text -> assertThat(text).doesNotContain("64", "INTERNAL"));
    }

    @Test
    void assignmentPublicationAndGradingHooksAreDurable() throws Exception {
        ready();
        var x = body(mvc.perform(json(post("/api/assignments"), Map.of("title", "Auto homework", "batchId", batch.getId(), "maxMarks", 100, "dueDateTime", Instant.now().plusSeconds(3600).toString(), "allowLateSubmission", false), admin)).andExpect(status().isCreated()).andReturn());
        long id = x.get("id").asLong();
        mvc.perform(json(post("/api/assignments/" + id + "/publish"), Map.of("version", x.get("version").asLong()), admin)).andExpect(status().isOk());
        var s = body(mvc.perform(json(post("/api/assignments/" + id + "/submissions"), Map.of("submissionText", "Working"), studentToken)).andExpect(status().isCreated()).andReturn());
        mvc.perform(json(put("/api/assignments/submissions/" + s.get("id").asLong() + "/grade"), Map.of("version", s.get("version").asLong(), "obtainedMarks", 85, "feedback", "Published feedback"), admin)).andExpect(status().isOk());
        drain();
        assertThat(notifications.findAll().stream().map(Notification::getType)).containsExactlyInAnyOrder(NotificationType.ASSIGNMENT_CREATED, NotificationType.ASSIGNMENT_CREATED, NotificationType.ASSIGNMENT_GRADED, NotificationType.ASSIGNMENT_GRADED);
    }

    @Test
    void materialPublicationUsesExistingStudentEligibility() throws Exception {
        ready();
        var category = body(mvc.perform(json(post("/api/resources/categories"), Map.of("name", "Auto notes"), admin)).andExpect(status().isCreated()).andReturn());
        var m = body(mvc.perform(json(post("/api/resources/materials"), Map.of("title", "Notes", "courseId", course.getId(), "batchId", batch.getId(), "categoryId", category.get("id").asLong(), "resourceType", "LINK", "externalUrl", "https://example.org/notes"), admin)).andExpect(status().isCreated()).andReturn());
        mvc.perform(json(post("/api/resources/materials/" + m.get("id").asLong() + "/publish"), Map.of("version", m.get("version").asLong()), admin)).andExpect(status().isOk());
        drain();
        assertThat(notifications.count()).isEqualTo(1);
        assertThat(notifications.findAll().getFirst().getRecipient().getId()).isEqualTo(student.getUser().getId());
        assertThat(notifications.findAll().getFirst().getActionUrl()).isEqualTo("/resources/materials/" + m.get("id").asLong());
    }

    @Test
    void recordedPaymentNotifiesWithoutSensitiveTransactionText() throws Exception {
        ready();
        var account = feeAccount(student);
        var i = new FeeInstallment();
        i.setFeeAccount(account);
        i.setAmount(new BigDecimal("1000"));
        i.setInstallmentNumber(1);
        i.setDueDate(today);
        persist(i);
        mvc.perform(json(post("/api/fees/payments"), Map.of("feeAccountId", account.getId(), "amount", 200, "paymentDate", today.toString(), "paymentMethod", "CASH", "idempotencyKey", "auto-payment", "remarks", "PRIVATE CASH NOTE", "allocations", List.of(Map.of("installmentId", i.getId(), "amount", 200))), admin)).andExpect(status().isCreated());
        drain();
        assertThat(notifications.count()).isEqualTo(2);
        assertThat(notifications.findAll().stream().map(Notification::getMessage)).allSatisfy(message -> assertThat(message).doesNotContain("PRIVATE", "200"));
    }

    @Test
    void finalizedAttendanceIsOptInAndNeverSentForDraft() throws Exception {
        ready();
        mvc.perform(json(put("/api/notifications/preferences"), Map.of("announcementsEnabled", true, "examNotificationsEnabled", true, "assignmentNotificationsEnabled", true, "feeNotificationsEnabled", true, "materialNotificationsEnabled", true, "attendanceNotificationsEnabled", true), parent)).andExpect(status().isOk());
        var session = body(mvc.perform(json(post("/api/attendance/sessions"), Map.of("batchId", batch.getId(), "sessionDate", today.toString(), "startTime", "09:00", "endTime", "10:00"), admin)).andExpect(status().isCreated()).andReturn());
        long id = session.at("/session/id").asLong();
        var saved = body(mvc.perform(json(put("/api/attendance/sessions/" + id + "/records"), Map.of("version", session.at("/session/version").asLong(), "records", List.of(Map.of("studentId", student.getId(), "status", "PRESENT"))), admin)).andExpect(status().isOk()).andReturn());
        assertThat(jobs.count()).isZero();
        mvc.perform(json(post("/api/attendance/sessions/" + id + "/finalize"), Map.of("version", saved.at("/session/version").asLong()), admin)).andExpect(status().isOk());
        drain();
        assertThat(notifications.count()).isEqualTo(1);
        assertThat(notifications.findAll().getFirst().getRecipient().getId()).isEqualTo(parentUser.getId());
        assertThat(notifications.findAll().getFirst().getType()).isEqualTo(NotificationType.ATTENDANCE_FINALIZED);
    }
}
