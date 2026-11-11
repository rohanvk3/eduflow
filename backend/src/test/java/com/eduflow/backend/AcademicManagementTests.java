package com.eduflow.backend;

import com.eduflow.backend.entity.*;
import com.eduflow.backend.repository.*;
import com.eduflow.backend.security.JwtService;
import com.eduflow.backend.service.BatchService;
import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class AcademicManagementTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired JwtService jwt;
    @Autowired CourseRepository courses; @Autowired BatchRepository batches;
    @Autowired BatchTeacherAssignmentRepository assignments; @Autowired BatchScheduleRepository schedules;
    @Autowired BatchEnrollmentRepository enrollments; @Autowired StudentRepository students;
    @Autowired TeacherRepository teachers; @Autowired UserRepository users;
    String admin,teacherToken,otherTeacherToken,studentToken,parentToken;
    Student student,secondStudent; Teacher teacher,otherTeacher;
    LocalDate today=BatchService.today(); Long courseId;
    @BeforeEach void setup() throws Exception {
        cleanup();
        users.saveAndFlush(new User("Admin","admin@academic.test","unused",Role.ADMIN));
        User teacherUser=users.saveAndFlush(new User("Teacher","teacher@academic.test","unused",Role.TEACHER));
        User otherUser=users.saveAndFlush(new User("Other","other@academic.test","unused",Role.TEACHER));
        User studentUser=users.saveAndFlush(new User("Student","student@academic.test","unused",Role.STUDENT));
        users.saveAndFlush(new User("Parent","parent@academic.test","unused",Role.PARENT));
        teacher=teacher("T-1",teacherUser);otherTeacher=teacher("T-2",otherUser);
        student=student("S-1",studentUser);secondStudent=student("S-2",null);
        admin=jwt.generateToken("admin@academic.test"); teacherToken=jwt.generateToken(teacherUser.getEmail());otherTeacherToken=jwt.generateToken(otherUser.getEmail());studentToken=jwt.generateToken(studentUser.getEmail());parentToken=jwt.generateToken("parent@academic.test");
        courseId=created("/api/courses",Map.of("code","MATH","name","Mathematics","description","Course description","durationWeeks",12)).get("id").asLong();
    }
    @AfterEach void cleanup() { schedules.deleteAll();assignments.deleteAll();enrollments.deleteAll();batches.deleteAll();courses.deleteAll();teachers.deleteAll();students.deleteAll();users.deleteAll(); }
    Teacher teacher(String code,User user) { Teacher t=new Teacher();t.setEmployeeId(code);t.setEmail(code+"@faculty.test");t.setFullName(code);t.setUser(user);teachers.saveAndFlush(t);return t; }
    Student student(String code,User user) { Student s=new Student(code);s.setFullName(code);s.setEmail(code+"@student.test");s.setPhone("9876543210");s.setDateOfBirth(today.minusYears(18));s.setAddress("Address");s.setGuardianName("Guardian");s.setGuardianPhone("9876543211");s.setAdmissionDate(today.minusMonths(3));s.setUser(user);return students.saveAndFlush(s); }
    JsonNode created(String url,Object body) throws Exception { return mapper.readTree(mvc.perform(json(post(url),body,admin)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()); }
    MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request,Object body,String token) throws Exception { return request.header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)); }
    Map<String,Object> batchBody(String code,int capacity,LocalDate start,LocalDate end) { return new HashMap<>(Map.of("code",code,"name",code+" batch","courseId",courseId,"capacity",capacity,"startDate",start.toString(),"endDate",end.toString(),"status","ACTIVE")); }
    Long batch(String code,int capacity) throws Exception { return created("/api/batches",batchBody(code,capacity,today.minusMonths(1),today.plusMonths(1))).get("id").asLong(); }
    JsonNode enroll(Long batch,Student s,LocalDate date) throws Exception { return created("/api/batches/"+batch+"/enrollments",Map.of("studentId",s.getId(),"enrollmentDate",date.toString())); }
    void assign(Long batch,Teacher t) throws Exception { created("/api/batches/"+batch+"/teachers",Map.of("teacherId",t.getId())); }
    Map<String,Object> slot(Teacher t,String start,String end) { return Map.of("teacherId",t.getId(),"dayOfWeek","MONDAY","startTime",start,"endTime",end,"room","A1","active",true); }

    @Test void courseCrudUniquenessValidationAndSoftStatus() throws Exception {
        mvc.perform(get("/api/courses").header("Authorization","Bearer "+admin).param("search","math")).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1));
        mvc.perform(json(post("/api/courses"),Map.of("code","math","name","Other","durationWeeks",3),admin)).andExpect(status().isConflict());
        mvc.perform(json(post("/api/courses"),Map.of("code","bad code","name"," ","durationWeeks",0),admin)).andExpect(status().isBadRequest());
        mvc.perform(json(put("/api/courses/"+courseId),Map.of("code","MATH","name","Updated","durationWeeks",15),admin)).andExpect(status().isOk()).andExpect(jsonPath("name").value("Updated"));
        mvc.perform(patch("/api/courses/"+courseId+"/deactivate").header("Authorization","Bearer "+admin)).andExpect(status().isOk());
        mvc.perform(get("/api/courses").header("Authorization","Bearer "+studentToken)).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(0));
        mvc.perform(json(post("/api/batches"),batchBody("NO",10,today,today.plusMonths(1)),admin)).andExpect(status().isConflict());
        assertThat(courses.count()).isEqualTo(1);
    }
    @Test void batchCreationValidationPaginationAndCourseLink() throws Exception {
        Long id=batch("B1",2);batch("B2",3);
        mvc.perform(get("/api/batches").param("size","1").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(2)).andExpect(jsonPath("content.length()").value(1));
        mvc.perform(get("/api/batches/"+id).header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("courseName").value("Mathematics"));
        mvc.perform(json(post("/api/batches"),batchBody("B1",4,today,today.plusDays(2)),admin)).andExpect(status().isConflict());
        mvc.perform(json(post("/api/batches"),batchBody("BAD",0,today,today.minusDays(1)),admin)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/batches").param("size","101").header("Authorization","Bearer "+admin)).andExpect(status().isBadRequest());
    }
    @Test void roleScopeProtectsBatchAndRoster() throws Exception {
        Long b=batch("OWN",10),other=batch("OTHER",10);assign(b,teacher);enroll(b,student,today.minusDays(1));
        for(String token:List.of(teacherToken,studentToken)) {
            mvc.perform(get("/api/batches").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1));
            mvc.perform(get("/api/batches/"+other).header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
            mvc.perform(json(post("/api/batches"),batchBody("DENIED",5,today,today),token)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/batches/"+b+"/enrollments").header("Authorization","Bearer "+teacherToken)).andExpect(status().isOk());
        mvc.perform(get("/api/batches/"+b+"/enrollments").header("Authorization","Bearer "+studentToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/batches/my-enrollments").param("studentId",secondStudent.getId().toString()).header("Authorization","Bearer "+studentToken)).andExpect(status().isOk()).andExpect(jsonPath("content[0].studentId").value(student.getId()));
        mvc.perform(get("/api/batches").header("Authorization","Bearer "+parentToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/courses")).andExpect(status().isUnauthorized());
    }
    @Test void capacityDuplicateAndHistoryEligibility() throws Exception {
        Long b=batch("HISTORY",1);Long enrollment=enroll(b,student,today.minusDays(5)).get("id").asLong();
        mvc.perform(json(post("/api/batches/"+b+"/enrollments"),Map.of("studentId",student.getId(),"enrollmentDate",today.toString()),admin)).andExpect(status().isConflict());
        mvc.perform(json(post("/api/batches/"+b+"/enrollments"),Map.of("studentId",secondStudent.getId(),"enrollmentDate",today.toString()),admin)).andExpect(status().isConflict());
        mvc.perform(json(patch("/api/batches/"+b+"/enrollments/"+enrollment+"/withdraw"),Map.of("withdrawalDate",today.toString()),admin)).andExpect(status().isOk());
        assertThat(enrollments.findEligibleOn(b,today.minusDays(3))).hasSize(1);
        assertThat(enrollments.findEligibleOn(b,today)).isEmpty();
        assertThat(enrollments.findEligibleOn(b,today.minusDays(6))).isEmpty();
        enroll(b,secondStudent,today);
        assertThat(enrollments.count()).isEqualTo(2);
    }
    @Test void backdatedWithdrawalAndInvalidEnrollmentRejected() throws Exception {
        Long b=batch("DATES",5);Long e=enroll(b,student,today.minusDays(2)).get("id").asLong();
        mvc.perform(json(patch("/api/batches/"+b+"/enrollments/"+e+"/withdraw"),Map.of("withdrawalDate",today.minusDays(1).toString()),admin)).andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/batches/"+b+"/enrollments"),Map.of("studentId",secondStudent.getId(),"enrollmentDate",today.plusDays(1).toString()),admin)).andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/batches/"+b+"/enrollments"),Map.of("studentId",999999,"enrollmentDate",today.toString()),admin)).andExpect(status().isNotFound());
        secondStudent.deactivate();students.saveAndFlush(secondStudent);
        mvc.perform(json(post("/api/batches/"+b+"/enrollments"),Map.of("studentId",secondStudent.getId(),"enrollmentDate",today.toString()),admin)).andExpect(status().isConflict());
    }
    @Test void reEnrollmentCreatesNewNonOverlappingPeriod() throws Exception {
        Long b=batch("REJOIN",5);
        BatchEnrollment old=new BatchEnrollment();old.setBatch(batches.findById(b).orElseThrow());old.setStudent(student);old.setEnrollmentDate(today.minusDays(10));old.setWithdrawalDate(today.minusDays(3));old.setStatus(EnrollmentStatus.WITHDRAWN);enrollments.saveAndFlush(old);
        enroll(b,student,today.minusDays(1));
        assertThat(enrollments.findByBatchIdAndStudentIdOrderByEnrollmentDate(b,student.getId())).hasSize(2);
        assertThat(enrollments.findEligibleOn(b,today.minusDays(2))).isEmpty();
        assertThat(enrollments.findEligibleOn(b,today)).hasSize(1);
    }
    @Test void historyFreezesDatesAndCapacityCannotShrink() throws Exception {
        Long b=batch("FIXED",2);enroll(b,student,today);enroll(b,secondStudent,today);
        var update=batchBody("FIXED",1,today.minusMonths(1),today.plusMonths(1));
        mvc.perform(json(put("/api/batches/"+b),update,admin)).andExpect(status().isConflict());
        update.put("capacity",2);update.put("startDate",today.minusMonths(2).toString());
        mvc.perform(json(put("/api/batches/"+b),update,admin)).andExpect(status().isConflict());
    }
    @Test void completionPreservesEligibilityAndLocksBatch() throws Exception {
        var body=batchBody("DONE",2,today.minusMonths(1),today.minusDays(1));
        Long b=created("/api/batches",body).get("id").asLong();enroll(b,student,today.minusDays(10));body.put("status","COMPLETED");
        mvc.perform(json(put("/api/batches/"+b),body,admin)).andExpect(status().isOk());
        assertThat(enrollments.findEligibleOn(b,today.minusDays(2))).hasSize(1);
        assertThat(enrollments.findEligibleOn(b,today)).isEmpty();
        mvc.perform(json(post("/api/batches/"+b+"/teachers"),Map.of("teacherId",teacher.getId()),admin)).andExpect(status().isConflict());
        mvc.perform(json(put("/api/batches/"+b),body,admin)).andExpect(status().isConflict());
    }
    @Test void assignmentsSchedulesOverlapAndUnassignment() throws Exception {
        Long b=batch("TIMETABLE",5),other=batch("OTHER",5);assign(b,teacher);assign(other,teacher);assign(b,otherTeacher);
        mvc.perform(json(post("/api/batches/"+b+"/teachers"),Map.of("teacherId",teacher.getId()),admin)).andExpect(status().isConflict());
        Long schedule=created("/api/batches/"+b+"/schedules",slot(teacher,"09:00","10:00")).get("id").asLong();
        mvc.perform(json(post("/api/batches/"+other+"/schedules"),slot(teacher,"09:30","10:30"),admin)).andExpect(status().isConflict());
        mvc.perform(json(post("/api/batches/"+b+"/schedules"),slot(otherTeacher,"09:30","10:30"),admin)).andExpect(status().isConflict());
        created("/api/batches/"+other+"/schedules",slot(teacher,"10:00","11:00"));
        mvc.perform(json(put("/api/batches/"+b+"/schedules/"+schedule),slot(teacher,"11:00","12:00"),admin)).andExpect(status().isOk());
        mvc.perform(delete("/api/batches/"+b+"/teachers/"+teacher.getId()).header("Authorization","Bearer "+admin)).andExpect(status().isOk());
        assertThat(schedules.findById(schedule).orElseThrow().getActive()).isFalse();
        mvc.perform(get("/api/batches/"+b).header("Authorization","Bearer "+teacherToken)).andExpect(status().isForbidden());
    }
    @Test void invalidTimesAndUnassignedTeacherRejected() throws Exception {
        Long b=batch("INVALID",5);assign(b,teacher);
        mvc.perform(json(post("/api/batches/"+b+"/schedules"),slot(teacher,"10:00","09:00"),admin)).andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/batches/"+b+"/schedules"),slot(otherTeacher,"09:00","10:00"),admin)).andExpect(status().isConflict());
        mvc.perform(json(post("/api/batches/"+b+"/schedules"),slot(teacher,"09:00:30","10:00"),admin)).andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/batches/"+b+"/schedules"),slot(teacher,"09:00","10:00"),teacherToken)).andExpect(status().isForbidden());
    }
    @Test void capacityIsSafeUnderConcurrentEnrollment() throws Exception {
        Long b=batch("RACE",1);CountDownLatch start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results=new ArrayList<>();
            for(Student s:List.of(student,secondStudent)) results.add(pool.submit(()->{start.await();return mvc.perform(json(post("/api/batches/"+b+"/enrollments"),Map.of("studentId",s.getId(),"enrollmentDate",today.toString()),admin)).andReturn().getResponse().getStatus();}));
            start.countDown();assertThat(List.of(results.get(0).get(10,TimeUnit.SECONDS),results.get(1).get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
        }
        assertThat(enrollments.countByBatchIdAndStatus(b,EnrollmentStatus.ACTIVE)).isEqualTo(1);
    }
    @Test void teacherConflictsAreSafeAcrossConcurrentBatches() throws Exception {
        Long b=batch("RACE1",2),other=batch("RACE2",2);assign(b,teacher);assign(other,teacher);CountDownLatch start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results=new ArrayList<>();
            for(Long id:List.of(b,other)) results.add(pool.submit(()->{start.await();return mvc.perform(json(post("/api/batches/"+id+"/schedules"),slot(teacher,"09:00","10:00"),admin)).andReturn().getResponse().getStatus();}));
            start.countDown();assertThat(List.of(results.get(0).get(10,TimeUnit.SECONDS),results.get(1).get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
        }
    }
}
