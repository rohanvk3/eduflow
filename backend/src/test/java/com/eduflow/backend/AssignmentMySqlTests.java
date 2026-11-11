package com.eduflow.backend;
import com.eduflow.backend.entity.*;
import java.time.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicReference;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@EnabledIfEnvironmentVariable(named="EDUFLOW_MYSQL_TEST",matches="true")
@org.springframework.test.context.ContextConfiguration(initializers = IsolatedMySqlInitializer.class)
@SpringBootTest(properties={"spring.datasource.url=${EDUFLOW_TEST_DB_URL}","spring.datasource.username=${EDUFLOW_TEST_DB_USERNAME}","spring.datasource.password=${EDUFLOW_TEST_DB_PASSWORD}","spring.jpa.hibernate.ddl-auto=update","app.jwt.secret=dGVzdC1vbmx5LXNlY3JldC1rZXktMzItYnl0ZXMtbG9uZyE=","app.exams.expiry-enabled=false"})
class AssignmentMySqlTests extends AcademicMySqlTests {
 static final Path storage;
 static{try{storage=Files.createTempDirectory("eduflow-homework-mysql-");}catch(IOException ex){throw new ExceptionInInitializerError(ex);}}
 @DynamicPropertySource static void storage(DynamicPropertyRegistry properties){properties.add("app.assignments.storage-dir",()->storage.toString());}
 @MockitoBean(name="assignmentClock")Clock clock;
 @Autowired JdbcTemplate jdbc;
 @AfterTransaction void filesRolledBack()throws Exception{try(var files=Files.list(storage)){assertThat(files.count()).isZero();}}
 long assignmentVersion(long id)throws Exception{return mapper.readTree(mvc.perform(get("/api/assignments/"+id).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("version").asLong();}
 @Test void mysqlAttachmentsSubmissionsRevisionsGradingAndStatistics()throws Exception{
  Instant initial=Instant.now();var now=new AtomicReference<>(initial);when(clock.instant()).thenAnswer(x->now.get());LocalDate today=initial.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();String tag=UUID.randomUUID().toString().replace("-","");
  var admin=users.saveAndFlush(new User("Homework QA",tag+"@admin.invalid","unused",Role.ADMIN));token=jwt.generateToken(admin.getEmail());
  var login=users.saveAndFlush(new User("Homework student",tag+"@login.invalid","unused",Role.STUDENT));String studentToken=jwt.generateToken(login.getEmail());
  var s=new Student("QA-"+tag);s.setFullName("Homework QA");s.setEmail(tag+"@student.invalid");s.setPhone("9876543210");s.setDateOfBirth(today.minusYears(18));s.setAdmissionDate(today.minusDays(8));s.setAddress("QA");s.setGuardianName("Guardian");s.setGuardianPhone("9876543211");s.setUser(login);students.saveAndFlush(s);
  long c=create("/api/courses",Map.of("code","QA-"+tag,"name","Homework QA","durationWeeks",12));
  long b=create("/api/batches",Map.of("code","QA-"+tag,"name","Homework QA batch","courseId",c,"capacity",5,"startDate",today.minusDays(5).toString(),"endDate",today.plusMonths(1).toString(),"status","ACTIVE"));
  create("/api/batches/"+b+"/enrollments",Map.of("studentId",s.getId(),"enrollmentDate",today.minusDays(2).toString()));
  long id=create("/api/assignments",Map.of("title","Homework "+tag,"batchId",b,"maxMarks",100,"dueDateTime",initial.plusSeconds(3600).toString(),"allowLateSubmission",false));
  var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",out);byte[] image=out.toByteArray();
  var teaching=mapper.readTree(mvc.perform(multipart("/api/assignments/"+id+"/attachments").file(new MockMultipartFile("files","worksheet.png","image/png",image)).param("version",Long.toString(assignmentVersion(id))).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  mvc.perform(post("/api/assignments/"+id+"/publish").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",assignmentVersion(id))))).andExpect(status().isOk()).andExpect(jsonPath("counts.eligible").value(1));
  mvc.perform(get("/api/assignments/attachments/"+teaching.get(0).get("id").asLong()+"/download").header("Authorization","Bearer "+studentToken)).andExpect(status().isOk());
  now.set(initial.plusSeconds(3601));
  mvc.perform(get("/api/assignments/stats").param("batchId",Long.toString(b)).header("Authorization","Bearer "+studentToken)).andExpect(status().isOk()).andExpect(jsonPath("overdueAssignments").value(1));
  now.set(initial);
  var submitted=mapper.readTree(mvc.perform(multipart("/api/assignments/"+id+"/submissions").file(new MockMultipartFile("data","data","application/json","{\"submissionText\":\"\"}".getBytes())).file(new MockMultipartFile("files","answer.png","image/png",image)).header("Authorization","Bearer "+studentToken)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
  long sid=submitted.get("id").asLong();
  jdbc.update("update assignment_submissions set created_at=? where id=?",LocalDateTime.ofInstant(initial,ZoneId.of("Asia/Kolkata")),sid);
  var revised=mapper.readTree(mvc.perform(put("/api/assignments/submissions/"+sid).header("Authorization","Bearer "+studentToken).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",submitted.get("version").asLong(),"submissionText","Revised working")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  var graded=mapper.readTree(mvc.perform(put("/api/assignments/submissions/"+sid+"/grade").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",revised.get("version").asLong(),"obtainedMarks",85,"feedback","Good explanation")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  mvc.perform(put("/api/assignments/submissions/"+sid+"/grade").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",graded.get("version").asLong(),"obtainedMarks",90,"feedback","Rechecked","reason","Rechecked paper")))).andExpect(status().isOk());
  mvc.perform(get("/api/assignments/submissions/"+sid+"/history").header("Authorization","Bearer "+studentToken)).andExpect(status().isOk()).andExpect(jsonPath("revisions.length()").value(1)).andExpect(jsonPath("grades.length()").value(2));
  mvc.perform(get("/api/assignments/stats").param("batchId",Long.toString(b)).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("averageMarks").value(90)).andExpect(jsonPath("completionPercentage").value(100)).andExpect(jsonPath("submittedAssignments").value(1)).andExpect(jsonPath("activity[0].submissions").value(1));
  mvc.perform(get("/api/assignments").param("pendingOnly","true").header("Authorization","Bearer "+studentToken)).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(0));
  mvc.perform(get("/api/assignments/stats").param("batchId",Long.toString(b)).header("Authorization","Bearer "+studentToken)).andExpect(status().isOk()).andExpect(jsonPath("upcoming").isEmpty());
 }
}
