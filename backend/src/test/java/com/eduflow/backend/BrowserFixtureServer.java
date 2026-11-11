package com.eduflow.backend;

import com.eduflow.backend.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Test-classpath-only launcher. Never included in the production JAR. */
public class BrowserFixtureServer {
    public static void main(String[] args) throws Exception {
        String url = System.getenv("EDUFLOW_TEST_DB_URL");
        if (url == null || !url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/eduflow_test_browser_[a-z0-9]+"))
            throw new IllegalStateException("Browser fixtures require the dedicated loopback eduflow_test_browser schema");
        Path run = Path.of(Objects.requireNonNull(System.getenv("EDUFLOW_VERIFICATION_DIR"))).toAbsolutePath();
        String password = Objects.requireNonNull(System.getenv("EDUFLOW_E2E_PASSWORD"));
        var app = new SpringApplication(EduFlowApplication.class);
        app.addInitializers(c -> c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolated-browser", Map.of(
            "spring.datasource.url", url,
            "spring.datasource.username", "eduflow_test",
            "spring.datasource.password", Objects.requireNonNull(System.getenv("EDUFLOW_TEST_DB_PASSWORD")),
            "spring.jpa.hibernate.ddl-auto", "update",
            "server.address", "127.0.0.1", "server.port", "18081",
            "app.assignments.storage-dir", run.resolve("files").toString(),
            "app.cors.allowed-origins", "http://127.0.0.1:15173",
            "app.notifications.scheduler-enabled", "false", "app.exams.expiry-enabled", "false"))));
        var context = app.run();
        var tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        var em = context.getBean(EntityManager.class);
        var encoder = context.getBean(PasswordEncoder.class);
        Map<String,Object> fixture = tx.execute(status -> {
            if (em.createQuery("select count(u) from User u", Long.class).getSingleResult() != 0)
                throw new IllegalStateException("Refusing to seed a nonempty browser database");
            var admin = user(em, encoder, password, "admin", Role.ADMIN);
            var teacher = user(em, encoder, password, "teacher", Role.TEACHER);
            var student = user(em, encoder, password, "student", Role.STUDENT);
            var other = user(em, encoder, password, "other", Role.STUDENT);
            var parent = user(em, encoder, password, "parent", Role.PARENT);
            var pending = user(em, encoder, password, "pending", Role.PARENT);
            var course = new Course(); course.setCode("E2E-MATH"); course.setName("E2E Mathematics"); course.setDurationWeeks(12); em.persist(course);
            var today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
            var batch = new Batch(); batch.setCode("E2E-BATCH"); batch.setName("E2E Morning Batch"); batch.setCourse(course); batch.setCapacity(20); batch.setStartDate(today.minusDays(20)); batch.setEndDate(today.plusMonths(3)); batch.setStatus(BatchStatus.ACTIVE); em.persist(batch);
            var faculty = new Teacher(); faculty.setFullName("E2E Teacher"); faculty.setEmail(teacher.getEmail()); faculty.setEmployeeId("E2E-T"); faculty.setUser(teacher); em.persist(faculty);
            var assignment = new BatchTeacherAssignment(); assignment.setBatch(batch); assignment.setTeacher(faculty); em.persist(assignment);
            var child = student(em, "E2E-ONE", student, today); var unrelated = student(em, "E2E-TWO", other, today);
            var enrollment = new BatchEnrollment(); enrollment.setBatch(batch); enrollment.setStudent(child); enrollment.setEnrollmentDate(today.minusDays(10)); em.persist(enrollment);
            link(em, parent, child, ParentLinkStatus.VERIFIED, admin); link(em, pending, child, ParentLinkStatus.PENDING, admin);
            em.flush();
            var result = new LinkedHashMap<String,Object>();
            result.put("password", password); result.put("courseId", course.getId()); result.put("batchId", batch.getId()); result.put("teacherId", faculty.getId()); result.put("studentId", child.getId()); result.put("otherStudentId", unrelated.getId()); result.put("enrollmentId", enrollment.getId()); result.put("today", today.toString());
            return result;
        });
        context.getBean(ObjectMapper.class).writeValue(run.resolve("fixtures.json").toFile(), fixture);
        System.out.println("ISOLATED_BROWSER_FIXTURES_READY");
    }
    static User user(EntityManager em, PasswordEncoder encoder, String password, String name, Role role) {
        var user = new User("E2E " + name, name + "@e2e.invalid", encoder.encode(password), role); em.persist(user); return user;
    }
    static Student student(EntityManager em, String code, User user, LocalDate today) {
        var s = new Student(code); s.setFullName(code); s.setEmail(user.getEmail()); s.setPhone("9876543210"); s.setDateOfBirth(today.minusYears(18)); s.setAdmissionDate(today.minusDays(15)); s.setAddress("Synthetic E2E fixture"); s.setGuardianName("E2E Guardian"); s.setGuardianPhone("9876543211"); s.setUser(user); em.persist(s); return s;
    }
    static void link(EntityManager em, User user, Student child, ParentLinkStatus status, User admin) {
        var p = new ParentProfile(); p.setUser(user); p.setPhoneNumber("9876543210"); em.persist(p);
        var link = new ParentStudentLink(); link.setParent(p); link.setStudent(child); link.setRelationshipType(RelationshipType.GUARDIAN); link.setStatus(status);
        if (status == ParentLinkStatus.VERIFIED) { link.setVerifiedBy(admin); link.setVerifiedAt(Instant.now()); }
        em.persist(link);
    }
}
