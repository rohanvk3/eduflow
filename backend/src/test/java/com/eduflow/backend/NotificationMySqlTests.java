package com.eduflow.backend;

import com.eduflow.backend.entity.Role;
import com.eduflow.backend.entity.Student;
import com.eduflow.backend.entity.Teacher;
import com.eduflow.backend.entity.User;
import com.eduflow.backend.service.NotificationDeliveryService;
import com.eduflow.backend.service.NotificationReminderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfEnvironmentVariable(named = "EDUFLOW_MYSQL_TEST", matches = "true")
@org.springframework.test.context.ContextConfiguration(initializers = IsolatedMySqlInitializer.class)
@SpringBootTest(properties = {"spring.datasource.url=${EDUFLOW_TEST_DB_URL}", "spring.datasource.username=${EDUFLOW_TEST_DB_USERNAME}", "spring.datasource.password=${EDUFLOW_TEST_DB_PASSWORD}", "spring.jpa.hibernate.ddl-auto=update", "app.jwt.secret=dGVzdC1vbmx5LXNlY3JldC1rZXktMzItYnl0ZXMtbG9uZyE=", "app.exams.expiry-enabled=false"})
class NotificationMySqlTests extends AcademicMySqlTests {
    @Autowired
    NotificationDeliveryService delivery;
    @Autowired
    NotificationReminderService reminders;
    @MockitoBean(name = "notificationClock")
    Clock clock;

    @Test
    void mysqlAudienceOutboxPreferencesReadTrackingAndScheduledPublication() throws Exception {
        Instant initial = Instant.now();
        var time = new AtomicReference<>(initial);
        when(clock.instant()).thenAnswer(x -> time.get());
        LocalDate today = initial.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
        String tag = UUID.randomUUID().toString().replace("-", "");
        var admin = users.saveAndFlush(new User("Notify QA", tag + "@admin.invalid", "unused", Role.ADMIN));
        token = jwt.generateToken(admin.getEmail());
        var su = users.saveAndFlush(new User("Notify learner", tag + "@login.invalid", "unused", Role.STUDENT));
        String studentToken = jwt.generateToken(su.getEmail());
        var tu = users.saveAndFlush(new User("Notify teacher", tag + "@teacher-login.invalid", "unused", Role.TEACHER));
        String teacherToken = jwt.generateToken(tu.getEmail());
        var s = new Student("QA-" + tag);
        s.setFullName("Notify learner");
        s.setEmail(tag + "@student.invalid");
        s.setPhone("9876543210");
        s.setDateOfBirth(today.minusYears(18));
        s.setAdmissionDate(today.minusDays(5));
        s.setGuardianName("Guardian");
        s.setGuardianPhone("9876543211");
        s.setAddress("QA");
        s.setUser(su);
        students.saveAndFlush(s);
        var t = new Teacher();
        t.setFullName("Notify teacher");
        t.setEmployeeId("QA-" + tag);
        t.setEmail(tag + "@teacher.invalid");
        t.setUser(tu);
        teachers.saveAndFlush(t);
        long c = create("/api/courses", Map.of("code", "QA-" + tag, "name", "Notify QA", "durationWeeks", 12));
        long b = create("/api/batches", Map.of("code", "QA-" + tag, "name", "Notify QA", "courseId", c, "capacity", 10, "startDate", today.minusDays(5).toString(), "endDate", today.plusMonths(2).toString(), "status", "ACTIVE"));
        create("/api/batches/" + b + "/enrollments", Map.of("studentId", s.getId(), "enrollmentDate", today.minusDays(2).toString()));
        create("/api/batches/" + b + "/teachers", Map.of("teacherId", t.getId()));
        long profile = create("/api/parents", Map.of("fullName", "Notify guardian", "email", tag + "@parent.invalid", "password", "Testing@2026", "phoneNumber", "9876543210"));
        String parentToken = jwt.generateToken(tag + "@parent.invalid");
        long link = create("/api/parents/links", Map.of("parentId", profile, "studentId", s.getId(), "relationshipType", "GUARDIAN", "reason", "QA consent"));
        mvc.perform(post("/api/parents/links/" + link + "/verify").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version", 0, "reason", "Verified QA consent")))).andExpect(status().isOk());
        long a = create("/api/announcements", Map.of("title", "Notify QA", "content", "Private batch notice", "audienceType", "BATCH", "batchId", b));
        mvc.perform(post("/api/announcements/" + a + "/publish").header("Authorization", "Bearer " + teacherToken).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}")).andExpect(status().isOk()).andExpect(jsonPath("recipients").value(3));
        delivery.deliverBatch();
        delivery.deliverBatch();
        var page = mapper.readTree(mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + studentToken)).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1)).andReturn().getResponse().getContentAsString());
        long notification = page.at("/content/0/id").asLong();
        mvc.perform(get("/api/announcements/me").header("Authorization", "Bearer " + parentToken)).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1));
        mvc.perform(patch("/api/notifications/" + notification + "/read").header("Authorization", "Bearer " + teacherToken)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/notifications/" + notification + "/read").header("Authorization", "Bearer " + studentToken)).andExpect(status().isOk());
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + studentToken)).andExpect(jsonPath("count").value(0));
        mvc.perform(put("/api/notifications/preferences").header("Authorization", "Bearer " + studentToken).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("announcementsEnabled", false, "examNotificationsEnabled", true, "assignmentNotificationsEnabled", true, "feeNotificationsEnabled", true, "materialNotificationsEnabled", true, "attendanceNotificationsEnabled", false)))).andExpect(status().isOk());
        long scheduled = create("/api/announcements", Map.of("title", "Scheduled QA", "content", "Later notice", "audienceType", "BATCH", "batchId", b, "publishAt", initial.plusSeconds(60).toString()));
        mvc.perform(post("/api/announcements/" + scheduled + "/publish").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}")).andExpect(jsonPath("status").value("SCHEDULED"));
        reminders.scheduledAnnouncements();
        time.set(initial.plusSeconds(61));
        reminders.scheduledAnnouncements();
        reminders.scheduledAnnouncements();
        delivery.deliverBatch();
        delivery.deliverBatch();
        mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + studentToken)).andExpect(jsonPath("totalElements").value(1));
        mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + parentToken)).andExpect(jsonPath("totalElements").value(2));
        mvc.perform(get("/api/announcements/me").header("Authorization", "Bearer " + studentToken)).andExpect(jsonPath("totalElements").value(2));
        mvc.perform(get("/api/announcements/overview").header("Authorization", "Bearer " + teacherToken)).andExpect(status().isOk()).andExpect(jsonPath("recipients").value(6)).andExpect(jsonPath("delivered").value(5)).andExpect(jsonPath("skipped").value(1));
    }
}
