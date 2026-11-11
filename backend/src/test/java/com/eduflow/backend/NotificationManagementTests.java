package com.eduflow.backend;

import com.eduflow.backend.entity.*;
import com.eduflow.backend.repository.NotificationJobRepository;
import com.eduflow.backend.repository.NotificationRepository;
import com.eduflow.backend.repository.NotificationTargetRepository;
import com.eduflow.backend.repository.UserRepository;
import com.eduflow.backend.security.JwtService;
import com.eduflow.backend.service.NotificationDeliveryService;
import com.eduflow.backend.service.NotificationOutbox;
import com.eduflow.backend.service.NotificationReminderService;
import com.eduflow.backend.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:notifications-tests;MODE=MySQL;DB_CLOSE_DELAY=-1", "app.exams.expiry-enabled=false"})
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationManagementTests {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    JwtService jwt;
    @Autowired
    EntityManager em;
    @Autowired
    UserRepository users;
    @Autowired
    NotificationRepository notifications;
    @Autowired
    NotificationJobRepository jobs;
    @Autowired
    NotificationTargetRepository targets;
    @Autowired
    NotificationOutbox outbox;
    @Autowired
    NotificationDeliveryService delivery;
    @Autowired
    NotificationReminderService reminders;
    @MockitoBean(name = "notificationClock")
    Clock clock;
    final AtomicReference<Instant> time = new AtomicReference<>();
    User adminUser, teacherUser, studentUser, otherUser, parentUser;
    String admin, teacher, student, other, parent, pending, unassigned;
    Student s1, s2;
    Course course;
    Batch batch, otherBatch;
    ParentStudentLink guardian;
    LocalDate today;

    <T> T persist(T t) {
        em.persist(t);
        em.flush();
        return t;
    }

    User account(String name, Role role) {
        return users.saveAndFlush(new User(name, name + "@notification.test", "unused", role));
    }

    @BeforeEach
    void setup() {
        time.set(Instant.now());
        when(clock.instant()).thenAnswer(x -> time.get());
        today = time.get().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
        adminUser = account("admin", Role.ADMIN);
        admin = jwt.generateToken(adminUser.getEmail());
        teacherUser = account("teacher", Role.TEACHER);
        teacher = jwt.generateToken(teacherUser.getEmail());
        unassigned = jwt.generateToken(account("unassigned", Role.TEACHER).getEmail());
        studentUser = account("student", Role.STUDENT);
        student = jwt.generateToken(studentUser.getEmail());
        otherUser = account("other", Role.STUDENT);
        other = jwt.generateToken(otherUser.getEmail());
        parentUser = account("parent", Role.PARENT);
        parent = jwt.generateToken(parentUser.getEmail());
        var pu = account("pending", Role.PARENT);
        pending = jwt.generateToken(pu.getEmail());
        course = new Course();
        course.setCode("NOTIFY");
        course.setName("Physics");
        course.setDurationWeeks(12);
        persist(course);
        batch = makeBatch("ONE");
        otherBatch = makeBatch("TWO");
        s1 = makeStudent("ONE", studentUser, batch);
        s2 = makeStudent("TWO", otherUser, otherBatch);
        var t = new Teacher();
        t.setFullName("Teacher");
        t.setEmployeeId("NOTIFY");
        t.setEmail("faculty@notify.test");
        t.setUser(teacherUser);
        persist(t);
        var assignment = new BatchTeacherAssignment();
        assignment.setBatch(batch);
        assignment.setTeacher(t);
        persist(assignment);
        guardian = link(parentUser, s1, ParentLinkStatus.VERIFIED);
        link(pu, s1, ParentLinkStatus.PENDING);
    }

    Batch makeBatch(String name) {
        var b = new Batch();
        b.setCode(name);
        b.setName(name);
        b.setCourse(course);
        b.setCapacity(20);
        b.setStartDate(today.minusDays(10));
        b.setEndDate(today.plusMonths(6));
        b.setStatus(BatchStatus.ACTIVE);
        return persist(b);
    }

    Student makeStudent(String name, User u, Batch b) {
        var s = new Student(name);
        s.setFullName(name);
        s.setEmail(name + "@admission.test");
        s.setPhone("9876543210");
        s.setDateOfBirth(today.minusYears(18));
        s.setAdmissionDate(today.minusDays(10));
        s.setGuardianName("Guardian");
        s.setGuardianPhone("9876543211");
        s.setAddress("Address");
        s.setUser(u);
        persist(s);
        var n = new BatchEnrollment();
        n.setStudent(s);
        n.setBatch(b);
        n.setEnrollmentDate(today.minusDays(5));
        persist(n);
        return s;
    }

    ParentStudentLink link(User u, Student s, ParentLinkStatus status) {
        var p = em.createQuery("select p from ParentProfile p where p.user=:u", ParentProfile.class).setParameter("u", u).getResultStream().findFirst().orElse(null);
        if (p == null) {
            p = new ParentProfile();
            p.setUser(u);
            p.setPhoneNumber("9876543210");
            persist(p);
        }
        var l = new ParentStudentLink();
        l.setParent(p);
        l.setStudent(s);
        l.setRelationshipType(RelationshipType.GUARDIAN);
        l.setStatus(status);
        if (status == ParentLinkStatus.VERIFIED) {
            l.setVerifiedBy(adminUser);
            l.setVerifiedAt(time.get());
        }
        return persist(l);
    }

    MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, Object value, String token) throws Exception {
        return b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(value));
    }

    JsonNode body(MvcResult r) throws Exception {
        return mapper.readTree(r.getResponse().getContentAsString());
    }

    JsonNode announce(String token, Map<String, Object> input) throws Exception {
        return body(mvc.perform(json(post("/api/announcements"), input, token)).andExpect(status().isCreated()).andReturn());
    }

    Map<String, Object> batchInput() {
        return new HashMap<>(Map.of("title", "Class notice", "content", "Read the class notice.", "audienceType", "BATCH", "batchId", batch.getId()));
    }

    JsonNode publish(JsonNode a) throws Exception {
        return body(mvc.perform(json(post("/api/announcements/" + a.get("id").asLong() + "/publish"), Map.of("version", a.get("version").asLong()), admin)).andExpect(status().isOk()).andReturn());
    }

    void drain() {
        for (int i = 0; i < 100; i++) if (delivery.deliverBatch() == 0) return;
        throw new AssertionError("Delivery did not drain");
    }

    Notification ownNotification() {
        return notifications.findAll().stream().filter(n -> n.getRecipient().getId().equals(studentUser.getId())).findFirst().orElseThrow();
    }

    void general(User user, String key) {
        outbox.capture(key, NotificationType.GENERAL, "USER", user.getId(), null);
    }

    @Test
    void notificationsAreOwnedAndReadAllDoesNotTouchOthers() throws Exception {
        general(studentUser, "one");
        general(otherUser, "two");
        drain();
        var n = ownNotification();
        mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + student)).andExpect(jsonPath("totalElements").value(1));
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + student)).andExpect(jsonPath("count").value(1));
        mvc.perform(patch("/api/notifications/" + n.getId() + "/read").header("Authorization", "Bearer " + other)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/notifications/" + n.getId()).header("Authorization", "Bearer " + other)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/notifications/read-all").header("Authorization", "Bearer " + student)).andExpect(jsonPath("updated").value(1));
        em.clear();
        assertThat(notifications.findAll().stream().filter(x -> x.getRecipient().getId().equals(otherUser.getId())).findFirst().orElseThrow().getReadFlag()).isFalse();
    }

    @Test
    void readIsIdempotentAndUnreadCountIsPersisted() throws Exception {
        general(studentUser, "one");
        drain();
        long id = ownNotification().getId();
        for (int i = 0; i < 2; i++)
            mvc.perform(patch("/api/notifications/" + id + "/read").header("Authorization", "Bearer " + student)).andExpect(status().isOk()).andExpect(jsonPath("isRead").value(true)).andExpect(jsonPath("readAt").isNotEmpty());
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + student)).andExpect(jsonPath("count").value(0));
    }

    @Test
    void dismissalRetainsDeduplicationTombstoneAndPaginationWorks() throws Exception {
        general(studentUser, "one");
        general(studentUser, "two");
        drain();
        long id = ownNotification().getId();
        mvc.perform(get("/api/notifications").param("size", "1").header("Authorization", "Bearer " + student)).andExpect(jsonPath("totalPages").value(2));
        mvc.perform(delete("/api/notifications/" + id).header("Authorization", "Bearer " + student)).andExpect(status().isNoContent());
        general(studentUser, "one");
        drain();
        mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + student)).andExpect(jsonPath("totalElements").value(1));
        assertThat(notifications.count()).isEqualTo(2);
    }

    @Test
    void expiredNotificationsAreExcluded() throws Exception {
        general(studentUser, "one");
        drain();
        ownNotification().setExpiresAt(time.get().minusSeconds(1));
        em.flush();
        mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + student)).andExpect(jsonPath("totalElements").value(0));
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + student)).andExpect(jsonPath("count").value(0));
    }

    @Test
    void duplicateOutboxEventsCreateOnlyOneCopy() throws Exception {
        general(studentUser, "repeat");
        general(studentUser, "repeat");
        drain();
        general(studentUser, "repeat");
        drain();
        assertThat(jobs.count()).isEqualTo(1);
        assertThat(notifications.count()).isEqualTo(1);
    }

    @Test
    void safeActionsRejectExternalAndEncodedRedirects() {
        for (String url : List.of("https://bad.test", "//bad.test", "/notifications?next=https://bad.test", "/parent/../students", "/notifications/%2e%2e", "/notifications\\evil", "javascript:alert(1)"))
            assertThatThrownBy(() -> NotificationService.safeAction(url)).isInstanceOf(IllegalArgumentException.class);
        assertThat(NotificationService.safeAction("/parent/children/12/fees")).isEqualTo("/parent/children/12/fees");
    }

    @Test
    void draftIsPrivateAndTeacherCannotBroadenAudience() throws Exception {
        var n = announce(teacher, batchInput());
        mvc.perform(get("/api/announcements/" + n.get("id").asLong()).header("Authorization", "Bearer " + student)).andExpect(status().isForbidden());
        for (String role : List.of(student, parent, unassigned))
            mvc.perform(json(post("/api/announcements"), batchInput(), role)).andExpect(status().isForbidden());
        mvc.perform(json(post("/api/announcements"), Map.of("title", "No", "content", "No", "audienceType", "ALL"), teacher)).andExpect(status().isForbidden());
        mvc.perform(get("/api/announcements/me").header("Authorization", "Bearer " + student)).andExpect(jsonPath("totalElements").value(0));
    }

    @Test
    void invalidAudienceCombinationsFail() throws Exception {
        for (var input : List.of(Map.of("title", "X", "content", "X", "audienceType", "ROLE"), Map.of("title", "X", "content", "X", "audienceType", "ALL", "batchId", batch.getId()), Map.of("title", "X", "content", "X", "audienceType", "BATCH", "batchId", batch.getId(), "targetRole", "STUDENT")))
            mvc.perform(json(post("/api/announcements"), input, admin)).andExpect(status().isBadRequest());
    }

    @Test
    void batchPublicationSnapshotsStudentsTeachersAndVerifiedParents() throws Exception {
        var n = publish(announce(admin, batchInput()));
        assertThat(n.get("recipients").asLong()).isEqualTo(3);
        drain();
        assertThat(notifications.count()).isEqualTo(3);
        for (String token : List.of(student, teacher, parent))
            mvc.perform(get("/api/announcements/" + n.get("id").asLong()).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        for (String token : List.of(other, pending, unassigned))
            mvc.perform(get("/api/announcements/" + n.get("id").asLong()).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/announcements/me").header("Authorization", "Bearer " + parent)).andExpect(jsonPath("totalElements").value(1));
    }

    @Test
    void courseRecipientsAreDeduplicatedAcrossChildrenAndCurrentLinksRechecked() throws Exception {
        link(parentUser, s2, ParentLinkStatus.VERIFIED);
        var n = publish(announce(admin, Map.of("title", "Course notice", "content", "Notice", "audienceType", "COURSE", "courseId", course.getId())));
        assertThat(n.get("recipients").asLong()).isEqualTo(4);
        guardian.setStatus(ParentLinkStatus.REVOKED);
        em.flush();
        drain();
        assertThat(notifications.count()).isEqualTo(4);
        mvc.perform(get("/api/announcements/" + n.get("id").asLong()).header("Authorization", "Bearer " + parent)).andExpect(status().isOk());
    }

    @Test
    void revokedParentsLoseAnnouncementAndUndeliveredNotificationAccess() throws Exception {
        var n = publish(announce(admin, batchInput()));
        guardian.setStatus(ParentLinkStatus.REVOKED);
        em.flush();
        drain();
        assertThat(notifications.findAll().stream().noneMatch(x -> x.getRecipient().getId().equals(parentUser.getId()))).isTrue();
        mvc.perform(get("/api/announcements/" + n.get("id").asLong()).header("Authorization", "Bearer " + parent)).andExpect(status().isForbidden());
        mvc.perform(get("/api/announcements/me").header("Authorization", "Bearer " + parent)).andExpect(jsonPath("totalElements").value(0));
    }

    @Test
    void publicationSnapshotDoesNotGrantNewEnrolleesOldAnnouncements() throws Exception {
        var n = publish(announce(admin, batchInput()));
        var enrollment = new BatchEnrollment();
        enrollment.setStudent(s2);
        enrollment.setBatch(batch);
        enrollment.setEnrollmentDate(today);
        persist(enrollment);
        mvc.perform(get("/api/announcements/" + n.get("id").asLong()).header("Authorization", "Bearer " + other)).andExpect(status().isForbidden());
    }

    @Test
    void publishedContentIsFrozenAndArchiveIsStaffOnly() throws Exception {
        var n = publish(announce(admin, batchInput()));
        long id = n.get("id").asLong();
        var input = batchInput();
        input.put("version", n.get("version").asLong());
        mvc.perform(json(put("/api/announcements/" + id), input, admin)).andExpect(status().isConflict());
        mvc.perform(json(post("/api/announcements/" + id + "/archive"), Map.of("version", n.get("version").asLong()), teacher)).andExpect(status().isOk());
        drain();
        assertThat(notifications.count()).isZero();
        mvc.perform(get("/api/announcements/" + id).header("Authorization", "Bearer " + student)).andExpect(status().isForbidden());
    }

    @Test
    void schedulingWaitsForDueTimeAndRetriesDoNotDuplicate() throws Exception {
        var input = batchInput();
        input.put("publishAt", time.get().plusSeconds(60).toString());
        var n = publish(announce(admin, input));
        assertThat(n.get("status").asText()).isEqualTo("SCHEDULED");
        assertThat(reminders.scheduledAnnouncements()).isZero();
        assertThat(jobs.count()).isZero();
        time.set(time.get().plusSeconds(61));
        assertThat(reminders.scheduledAnnouncements()).isEqualTo(1);
        assertThat(reminders.scheduledAnnouncements()).isZero();
        drain();
        assertThat(notifications.count()).isEqualTo(3);
    }

    @Test
    void scheduledTeacherCannotPublishAfterAssignmentRevocation() throws Exception {
        var input = batchInput();
        input.put("publishAt", time.get().plusSeconds(60).toString());
        publish(announce(teacher, input));
        em.createQuery("update BatchTeacherAssignment a set a.active=false").executeUpdate();
        em.clear();
        time.set(time.get().plusSeconds(61));
        assertThat(reminders.scheduledAnnouncements()).isZero();
        assertThat(jobs.count()).isZero();
    }

    @Test
    void preferenceDefaultsAndOwnershipAreEnforced() throws Exception {
        mvc.perform(get("/api/notifications/preferences").header("Authorization", "Bearer " + parent)).andExpect(jsonPath("announcementsEnabled").value(true)).andExpect(jsonPath("attendanceNotificationsEnabled").value(false));
        var values = new HashMap<String, Object>(Map.of("announcementsEnabled", false, "examNotificationsEnabled", false, "assignmentNotificationsEnabled", false, "feeNotificationsEnabled", false, "materialNotificationsEnabled", false, "attendanceNotificationsEnabled", false));
        values.put("userId", otherUser.getId());
        mvc.perform(json(put("/api/notifications/preferences"), values, student)).andExpect(status().isOk());
        publish(announce(admin, batchInput()));
        general(studentUser, "mandatory");
        drain();
        assertThat(notifications.findAll().stream().filter(x -> x.getRecipient().getId().equals(studentUser.getId())).map(Notification::getType)).containsExactly(NotificationType.GENERAL);
        mvc.perform(get("/api/notifications/preferences").header("Authorization", "Bearer " + other)).andExpect(jsonPath("announcementsEnabled").value(true));
        mvc.perform(json(put("/api/notifications/preferences"), Map.of(), student)).andExpect(status().isBadRequest());
    }

    Exam exam(Instant start) {
        var e = new Exam();
        e.setExamCode(UUID.randomUUID().toString());
        e.setTitle("Exam");
        e.setBatch(batch);
        e.setExamType(ExamType.OFFLINE);
        e.setTotalMarks(new BigDecimal("100"));
        e.setPassingMarks(new BigDecimal("40"));
        e.setStartDateTime(start);
        e.setEndDateTime(start.plusSeconds(3600));
        e.setEligibilityDate(today);
        e.setCreatedBy(adminUser);
        e.setStatus(ExamStatus.PUBLISHED);
        return persist(e);
    }

    @Test
    void examReminderUsesWindowAndHistoricalEligibility() throws Exception {
        var soon = exam(time.get().plusSeconds(3600));
        exam(time.get().plus(Duration.ofDays(3)));
        reminders.exams();
        reminders.exams();
        drain();
        assertThat(jobs.count()).isEqualTo(1);
        assertThat(notifications.count()).isEqualTo(2);
        assertThat(notifications.findAll().stream().allMatch(n -> n.getRelatedEntityId().equals(soon.getId()))).isTrue();
    }

    Assignment assignment() {
        var x = new Assignment();
        x.setTitle("Homework");
        x.setBatch(batch);
        x.setCreatedBy(adminUser);
        x.setMaxMarks(new BigDecimal("100"));
        x.setDueDateTime(time.get().plusSeconds(7200));
        x.setPublishedAt(time.get());
        x.setStatus(AssignmentStatus.PUBLISHED);
        persist(x);
        var r = new AssignmentRecipient();
        r.setAssignment(x);
        r.setStudent(s1);
        persist(r);
        return x;
    }

    @Test
    void assignmentDueRemindersExcludeSubmittedAndAreDeduplicated() throws Exception {
        var x = assignment();
        reminders.assignments();
        reminders.assignments();
        drain();
        assertThat(notifications.count()).isEqualTo(2);
        var submitted = new AssignmentSubmission();
        submitted.setAssignment(x);
        submitted.setStudent(s1);
        submitted.setSubmissionText("Work");
        submitted.setSubmittedAt(time.get());
        persist(submitted);
        var next = assignment();
        var s = new AssignmentSubmission();
        s.setAssignment(next);
        s.setStudent(s1);
        s.setSubmissionText("Work");
        s.setSubmittedAt(time.get());
        persist(s);
        reminders.assignments();
        drain();
        assertThat(notifications.count()).isEqualTo(2);
    }

    FeeInstallment installment(LocalDate due) {
        var n = em.createQuery("select n from BatchEnrollment n where n.student=:s and n.batch=:b", BatchEnrollment.class).setParameter("s", s1).setParameter("b", batch).getSingleResult();
        var account = em.createQuery("select a from StudentFeeAccount a where a.enrollment=:n", StudentFeeAccount.class).setParameter("n", n).getResultStream().findFirst().orElse(null);
        if (account == null) {
            account = new StudentFeeAccount();
            account.setStudent(s1);
            account.setEnrollment(n);
            account.setStructureName("Agreed");
            account.setAgreementDate(today.minusDays(5));
            account.setTuitionFee(new BigDecimal("1000"));
            account.setAdmissionFee(BigDecimal.ZERO);
            account.setMaterialFee(BigDecimal.ZERO);
            account.setOtherFee(BigDecimal.ZERO);
            account.setOriginalAmount(new BigDecimal("1000"));
            account.setDiscountAmount(BigDecimal.ZERO);
            account.setFinalPayableAmount(new BigDecimal("1000"));
            persist(account);
        }
        var i = new FeeInstallment();
        i.setFeeAccount(account);
        i.setInstallmentNumber((int) em.createQuery("select count(i) from FeeInstallment i", Long.class).getSingleResult().longValue() + 1);
        i.setAmount(new BigDecimal("100"));
        i.setDueDate(due);
        return persist(i);
    }

    @Test
    void feeReminderUsesDueDateAndWeeklyOverdueWindow() throws Exception {
        installment(today.plusDays(2));
        installment(today.plusDays(4));
        installment(today.minusDays(1));
        reminders.fees();
        reminders.fees();
        drain();
        assertThat(notifications.count()).isEqualTo(4);
        assertThat(jobs.count()).isEqualTo(2);
        time.set(time.get().plus(Duration.ofDays(7)));
        reminders.fees();
        drain();
        assertThat(jobs.count()).isEqualTo(5);
    }

    @Test
    void settledInstallmentsAndVoidedPaymentsDoNotDeliver() throws Exception {
        var i = installment(today.plusDays(1));
        var p = new FeePayment();
        p.setFeeAccount(i.getFeeAccount());
        p.setAmount(i.getAmount());
        p.setPaymentDate(today);
        p.setPaymentMethod(PaymentMethod.CASH);
        p.setReceiptNumber("TEST");
        p.setIdempotencyKey("TEST");
        p.setRequestHash("TEST");
        p.setRecordedBy(adminUser);
        persist(p);
        var allocation = new PaymentAllocation();
        allocation.setPayment(p);
        allocation.setInstallment(i);
        allocation.setAmount(p.getAmount());
        persist(allocation);
        reminders.fees();
        assertThat(jobs.count()).isZero();
        outbox.capture("payment", NotificationType.PAYMENT_RECORDED, "PAYMENT", p.getId(), null);
        p.setStatus(FeePaymentStatus.VOIDED);
        em.flush();
        drain();
        assertThat(notifications.count()).isZero();
    }

    @Test
    void publicationHooksCreateDurableEventsInTheSameTransaction() throws Exception {
        var exam = mvc.perform(json(post("/api/exams"), Map.of("examCode", "AUTO", "title", "Offline", "batchId", batch.getId(), "examType", "OFFLINE", "totalMarks", 100, "passingMarks", 40, "startDateTime", time.get().plusSeconds(3600).toString(), "endDateTime", time.get().plusSeconds(7200).toString()), admin)).andExpect(status().isCreated()).andReturn();
        var e = body(exam);
        mvc.perform(json(post("/api/exams/" + e.get("id").asLong() + "/publish"), Map.of("version", e.get("version").asLong()), teacher)).andExpect(status().isOk());
        assertThat(jobs.findAll().stream().map(NotificationJob::getType)).contains(NotificationType.EXAM_SCHEDULED);
        drain();
        assertThat(notifications.count()).isEqualTo(2);
    }

    @Test
    void deliveryOverviewDoesNotExposeRecipients() throws Exception {
        publish(announce(teacher, batchInput()));
        drain();
        mvc.perform(get("/api/announcements/overview").header("Authorization", "Bearer " + teacher)).andExpect(status().isOk()).andExpect(jsonPath("recipients").value(3)).andExpect(jsonPath("delivered").value(3));
        mvc.perform(get("/api/announcements/overview").header("Authorization", "Bearer " + student)).andExpect(status().isForbidden());
    }

    @Test
    void authoredFilterKeepsCurrentAuthorizationScope() throws Exception {
        announce(teacher, batchInput());
        announce(admin, batchInput());
        mvc.perform(get("/api/announcements?createdByMe=true").header("Authorization", "Bearer " + teacher)).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1));
        mvc.perform(get("/api/announcements?createdByMe=true").header("Authorization", "Bearer " + student)).andExpect(jsonPath("totalElements").value(0));
        em.createQuery("update BatchTeacherAssignment a set a.active=false").executeUpdate();
        em.clear();
        mvc.perform(get("/api/announcements?createdByMe=true").header("Authorization", "Bearer " + teacher)).andExpect(jsonPath("totalElements").value(0));
    }

    @Test
    void unauthenticatedUsersCannotReadNotifications() throws Exception {
        mvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notifications/preferences")).andExpect(status().isUnauthorized());
    }
}
