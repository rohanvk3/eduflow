package com.eduflow.backend;

import com.eduflow.backend.entity.*;
import com.eduflow.backend.repository.*;
import com.eduflow.backend.security.JwtService;
import com.eduflow.backend.service.BatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opt-in real MySQL test. Only new UUID-tagged fixtures are written, then rolled back. */
@EnabledIfEnvironmentVariable(named="EDUFLOW_MYSQL_TEST",matches="true")
@org.springframework.test.context.ContextConfiguration(initializers = IsolatedMySqlInitializer.class)
@SpringBootTest(properties={"spring.datasource.url=${EDUFLOW_TEST_DB_URL}","spring.datasource.username=${EDUFLOW_TEST_DB_USERNAME}","spring.datasource.password=${EDUFLOW_TEST_DB_PASSWORD}","spring.jpa.hibernate.ddl-auto=update","app.jwt.secret=dGVzdC1vbmx5LXNlY3JldC1rZXktMzItYnl0ZXMtbG9uZyE="})
@AutoConfigureMockMvc @Transactional
class AcademicMySqlTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired UserRepository users;
    @Autowired TeacherRepository teachers; @Autowired StudentRepository students;
    @Autowired BatchEnrollmentRepository enrollments; @Autowired JwtService jwt;
    String token;
    long create(String url,Object body) throws Exception {
        String json=mvc.perform(post(url).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("id").asLong();
    }
    @Test void mysqlCourseBatchAssignmentEnrollmentAndTimetableRoundTrip() throws Exception {
        String suffix=UUID.randomUUID().toString().replace("-","");LocalDate today=BatchService.today();
        User admin=users.saveAndFlush(new User("MySQL test",suffix+"@qa.invalid","unused",Role.ADMIN));token=jwt.generateToken(admin.getEmail());
        Teacher t=new Teacher();t.setEmployeeId("QA-"+suffix);t.setEmail(suffix+"@teacher.invalid");t.setFullName("MySQL test teacher");teachers.saveAndFlush(t);
        Student s=new Student("QA-"+suffix);s.setFullName("MySQL test student");s.setEmail(suffix+"@student.invalid");s.setPhone("9876543210");s.setDateOfBirth(today.minusYears(18));s.setAdmissionDate(today.minusDays(5));s.setGuardianName("Guardian");s.setGuardianPhone("9876543211");s.setAddress("QA fixture");students.saveAndFlush(s);
        long c=create("/api/courses",Map.of("code","QA-"+suffix,"name","MySQL QA course","durationWeeks",12));
        long b=create("/api/batches",Map.of("code","QA-"+suffix,"name","MySQL QA batch","courseId",c,"capacity",10,"startDate",today.minusDays(7).toString(),"endDate",today.plusMonths(1).toString(),"status","ACTIVE"));
        create("/api/batches/"+b+"/teachers",Map.of("teacherId",t.getId()));
        long e=create("/api/batches/"+b+"/enrollments",Map.of("studentId",s.getId(),"enrollmentDate",today.minusDays(2).toString()));
        create("/api/batches/"+b+"/schedules",Map.of("teacherId",t.getId(),"dayOfWeek","MONDAY","startTime","09:00","endTime","10:00","active",true));
        mvc.perform(get("/api/batches/"+b+"/schedules").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$[0].teacherId").value(t.getId()));
        mvc.perform(patch("/api/batches/"+b+"/enrollments/"+e+"/withdraw").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("withdrawalDate",today.toString())))).andExpect(status().isOk());
        assertThat(enrollments.findEligibleOn(b,today.minusDays(1))).hasSize(1);
        assertThat(enrollments.findEligibleOn(b,today)).isEmpty();
    }
}
