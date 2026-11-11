package com.eduflow.backend;

import com.eduflow.backend.entity.*;
import com.eduflow.backend.repository.AttendanceRecordRepository;
import com.eduflow.backend.service.BatchService;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Inherits opt-in MySQL configuration and rollback transactions from the prerequisite smoke test. */
@EnabledIfEnvironmentVariable(named="EDUFLOW_MYSQL_TEST",matches="true")
class AttendanceMySqlTests extends AcademicMySqlTests {
    @Autowired AttendanceRecordRepository records;
    @Test void mysqlDraftFinalizationSummaryCsvAndPersonalHistory() throws Exception {
        String suffix=UUID.randomUUID().toString().replace("-","");LocalDate today=BatchService.today();
        User admin=users.saveAndFlush(new User("Attendance QA",suffix+"@qa.invalid","unused",Role.ADMIN));token=jwt.generateToken(admin.getEmail());
        User studentUser=users.saveAndFlush(new User("Attendance Student",suffix+"@login.invalid","unused",Role.STUDENT));
        Student s=new Student("QA-"+suffix);s.setFullName("=MySQL attendance QA");s.setEmail(suffix+"@student.invalid");s.setPhone("9876543210");s.setDateOfBirth(today.minusYears(18));s.setAdmissionDate(today.minusDays(5));s.setAddress("QA fixture");s.setGuardianName("Guardian");s.setGuardianPhone("9876543211");s.setUser(studentUser);students.saveAndFlush(s);
        long c=create("/api/courses",Map.of("code","QA-"+suffix,"name","Attendance QA","durationWeeks",12));
        long b=create("/api/batches",Map.of("code","QA-"+suffix,"name","Attendance QA batch","courseId",c,"capacity",5,"startDate",today.minusDays(7).toString(),"endDate",today.plusMonths(1).toString(),"status","ACTIVE"));
        create("/api/batches/"+b+"/enrollments",Map.of("studentId",s.getId(),"enrollmentDate",today.minusDays(2).toString()));
        String created=mvc.perform(post("/api/attendance/sessions").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("batchId",b,"sessionDate",today.toString(),"topic","MySQL smoke")))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long session=mapper.readTree(created).get("session").get("id").asLong();
        String saved=mvc.perform(put("/api/attendance/sessions/"+session+"/records").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",0,"records",List.of(Map.of("studentId",s.getId(),"status","LATE","remarks","Arrived late")))))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long version=mapper.readTree(saved).get("session").get("version").asLong();
        mvc.perform(post("/api/attendance/sessions/"+session+"/finalize").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",version)))).andExpect(status().isOk());
        mvc.perform(get("/api/attendance/reports").param("batchId",Long.toString(b)).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("totals.attendancePercentage").value(100.00)).andExpect(jsonPath("totals.lateCount").value(1));
        mvc.perform(get("/api/attendance/me").header("Authorization","Bearer "+jwt.generateToken(studentUser.getEmail()))).andExpect(status().isOk()).andExpect(jsonPath("history.content[0].status").value("LATE"));
        mvc.perform(get("/api/attendance/reports/export").param("batchId",Long.toString(b)).header("Authorization","Bearer "+token)).andExpect(status().isOk());
    }
}
