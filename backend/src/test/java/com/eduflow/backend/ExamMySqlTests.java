package com.eduflow.backend;
import com.eduflow.backend.entity.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/** Real MySQL; inherited transaction rolls back all UUID-tagged fixtures. */
@EnabledIfEnvironmentVariable(named="EDUFLOW_MYSQL_TEST",matches="true")
@org.springframework.test.context.ContextConfiguration(initializers = IsolatedMySqlInitializer.class)
@SpringBootTest(properties={"spring.datasource.url=${EDUFLOW_TEST_DB_URL}","spring.datasource.username=${EDUFLOW_TEST_DB_USERNAME}","spring.datasource.password=${EDUFLOW_TEST_DB_PASSWORD}","spring.jpa.hibernate.ddl-auto=update","app.jwt.secret=dGVzdC1vbmx5LXNlY3JldC1rZXktMzItYnl0ZXMtbG9uZyE=","app.exams.expiry-enabled=false"})
class ExamMySqlTests extends AcademicMySqlTests {
 @MockitoBean(name="examClock") Clock clock;
 @Test void mysqlOfflinePublicationCorrectionAndAnalytics()throws Exception{
  Instant initial=Instant.now();var time=new AtomicReference<>(initial);when(clock.instant()).thenAnswer(x->time.get());
  String tag=UUID.randomUUID().toString().replace("-","");LocalDate today=initial.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
  var u=users.saveAndFlush(new User("Exam QA",tag+"@exam.invalid","unused",Role.ADMIN));token=jwt.generateToken(u.getEmail());
  var s=new Student("QA-"+tag);s.setFullName("Exam QA");s.setEmail(tag+"@student.invalid");s.setPhone("9876543210");s.setDateOfBirth(today.minusYears(18));s.setAdmissionDate(today.minusDays(8));s.setAddress("QA");s.setGuardianName("Guardian");s.setGuardianPhone("9876543211");students.saveAndFlush(s);
  long c=create("/api/courses",Map.of("code","QA-"+tag,"name","Exam QA","durationWeeks",12));
  long b=create("/api/batches",Map.of("code","QA-"+tag,"name","Exam QA batch","courseId",c,"capacity",5,"startDate",today.minusDays(5).toString(),"endDate",today.plusMonths(1).toString(),"status","ACTIVE"));
  create("/api/batches/"+b+"/enrollments",Map.of("studentId",s.getId(),"enrollmentDate",today.minusDays(2).toString()));
  var input=new HashMap<String,Object>();input.put("examCode","QA-"+tag);input.put("title","Exam QA");input.put("batchId",b);input.put("examType","OFFLINE");input.put("totalMarks",10);input.put("passingMarks",5);input.put("startDateTime",initial.minusSeconds(30).toString());input.put("endDateTime",initial.plusSeconds(60).toString());
  long e=create("/api/exams",input);
  mvc.perform(post("/api/exams/"+e+"/publish").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}")).andExpect(status().isOk());
  time.set(initial.plusSeconds(61));
  String gradebook=mvc.perform(put("/api/exams/"+e+"/offline-results").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",1,"results",List.of(Map.of("studentId",s.getId(),"obtainedMarks",8)))))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  long version=mapper.readTree(gradebook).at("/exam/version").asLong();
  String published=mvc.perform(post("/api/exams/"+e+"/results/publish").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",version)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  mvc.perform(get("/api/exams/analytics").param("batchId",Long.toString(b)).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("scores.average").value(80));
  String results=mvc.perform(get("/api/exams/"+e+"/results").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  long result=mapper.readTree(results).at("/results/content/0/id").asLong();version=mapper.readTree(published).get("version").asLong();
  mvc.perform(post("/api/exams/"+e+"/results/"+result+"/correct").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",version,"obtainedMarks",9,"reason","Rollback-only recheck")))).andExpect(status().isOk());
  mvc.perform(get("/api/exams/analytics").param("batchId",Long.toString(b)).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("scores.average").value(90));
  var login=users.saveAndFlush(new User("Exam student",tag+"@login.invalid","unused",Role.STUDENT));s.setUser(login);students.saveAndFlush(s);
  input.put("examCode","MCQ-"+tag);input.put("examType","ONLINE_MCQ");input.put("durationMinutes",5);input.put("endDateTime",initial.plusSeconds(1800).toString());
  long online=create("/api/exams",input);
  var q=mapper.readTree(mvc.perform(post("/api/exams/"+online+"/questions").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",0,"questionText","Two plus two?","marks",10,"displayOrder",1,"options",List.of(Map.of("optionText","Four","correct",true),Map.of("optionText","Five","correct",false)))))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
  long onlineVersion=mapper.readTree(mvc.perform(get("/api/exams/"+online).header("Authorization","Bearer "+token)).andReturn().getResponse().getContentAsString()).get("version").asLong();
  mvc.perform(post("/api/exams/"+online+"/publish").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",onlineVersion)))).andExpect(status().isOk());
  String studentToken=jwt.generateToken(login.getEmail());
  var attempt=mapper.readTree(mvc.perform(post("/api/exams/"+online+"/attempts/start").header("Authorization","Bearer "+studentToken)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  long attemptId=attempt.get("id").asLong();
  mvc.perform(put("/api/exams/attempts/"+attemptId+"/answers").header("Authorization","Bearer "+studentToken).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",0,"answers",List.of(Map.of("questionId",q.get("id").asLong(),"selectedOptionId",q.at("/options/0/id").asLong())))))).andExpect(status().isOk());
  mvc.perform(post("/api/exams/attempts/"+attemptId+"/submit").header("Authorization","Bearer "+studentToken)).andExpect(status().isOk());
  time.set(initial.plusSeconds(1801));
  onlineVersion=mapper.readTree(mvc.perform(get("/api/exams/"+online).header("Authorization","Bearer "+token)).andReturn().getResponse().getContentAsString()).get("version").asLong();
  mvc.perform(post("/api/exams/"+online+"/results/publish").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",onlineVersion)))).andExpect(status().isOk());
  mvc.perform(get("/api/exams/"+online+"/result").header("Authorization","Bearer "+studentToken)).andExpect(status().isOk()).andExpect(jsonPath("result.obtainedMarks").value(10)).andExpect(jsonPath("review[0].correctOptionId").value(q.at("/options/0/id").asLong()));
 }
}
