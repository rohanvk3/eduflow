package com.eduflow.backend;
import com.eduflow.backend.entity.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@EnabledIfEnvironmentVariable(named="EDUFLOW_MYSQL_TEST",matches="true")
@org.springframework.test.context.ContextConfiguration(initializers = IsolatedMySqlInitializer.class)
@SpringBootTest(properties={"spring.datasource.url=${EDUFLOW_TEST_DB_URL}","spring.datasource.username=${EDUFLOW_TEST_DB_USERNAME}","spring.datasource.password=${EDUFLOW_TEST_DB_PASSWORD}","spring.jpa.hibernate.ddl-auto=update","app.jwt.secret=dGVzdC1vbmx5LXNlY3JldC1rZXktMzItYnl0ZXMtbG9uZyE=","app.exams.expiry-enabled=false"})
class ParentMySqlTests extends AcademicMySqlTests {
 @Autowired EntityManager em;
 @Test void mysqlVerifiedParentDashboardFinanceReceiptResultsAndRevocation()throws Exception{
  String tag=UUID.randomUUID().toString().replace("-","");LocalDate today=LocalDate.now(ZoneId.of("Asia/Kolkata"));Instant now=Instant.now();
  var admin=users.saveAndFlush(new User("Parent QA",tag+"@admin.invalid","unused",Role.ADMIN));token=jwt.generateToken(admin.getEmail());
  var s=new Student("QA-"+tag);s.setFullName("Parent child QA");s.setEmail(tag+"@child.invalid");s.setPhone("9876543210");s.setDateOfBirth(today.minusYears(18));s.setAdmissionDate(today.minusDays(10));s.setGuardianName("Guardian");s.setGuardianPhone("9876543211");s.setAddress("QA");students.saveAndFlush(s);
  long course=create("/api/courses",Map.of("code","QA-"+tag,"name","Parent QA","durationWeeks",12));
  long batchId=create("/api/batches",Map.of("code","QA-"+tag,"name","Parent QA","courseId",course,"capacity",5,"startDate",today.minusDays(10).toString(),"endDate",today.plusMonths(2).toString(),"status","ACTIVE"));
  long enrollment=create("/api/batches/"+batchId+"/enrollments",Map.of("studentId",s.getId(),"enrollmentDate",today.minusDays(5).toString()));
  long profile=create("/api/parents",Map.of("fullName","Parent QA","email",tag+"@parent.invalid","password","Testing@2026","phoneNumber","9876543210"));
  String login=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("email",tag+"@parent.invalid","password","Testing@2026")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String parent=mapper.readTree(login).get("token").asText();
  long link=create("/api/parents/links",Map.of("parentId",profile,"studentId",s.getId(),"relationshipType","FATHER","primaryGuardian",true,"reason","Identity documents checked"));
  mvc.perform(get("/api/parents/children/"+s.getId()+"/fees").header("Authorization","Bearer "+parent)).andExpect(status().isForbidden());
  var verified=mapper.readTree(mvc.perform(post("/api/parents/links/"+link+"/verify").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",0,"reason","Signed guardian consent verified")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  long structure=create("/api/fees/structures",Map.of("courseId",course,"name","Parent QA","tuitionFee",1000,"admissionFee",0,"materialFee",0,"otherFee",0,"effectiveFrom",today.minusDays(10).toString(),"status","ACTIVE"));
  var account=mapper.readTree(mvc.perform(post("/api/fees/accounts").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("enrollmentId",enrollment,"feeStructureId",structure,"discountAmount",100,"agreementDate",today.minusDays(5).toString())))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
  long aid=account.at("/account/id").asLong();
  var schedule=mapper.readTree(mvc.perform(post("/api/fees/accounts/"+aid+"/installments").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",account.at("/account/version").asLong(),"installments",List.of(Map.of("installmentNumber",1,"amount",900,"dueDate",today.minusDays(1).toString())))))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  long payment=create("/api/fees/payments",Map.of("feeAccountId",aid,"amount",300,"paymentDate",today.toString(),"paymentMethod","CASH","idempotencyKey",tag,"allocations",List.of(Map.of("installmentId",schedule.at("/installments/0/id").asLong(),"amount",300))));
  var batch=em.find(Batch.class,batchId);var session=new AttendanceSession();session.setBatch(batch);session.setSessionDate(today.minusDays(2));session.setOccurrenceKey("MANUAL-QA");session.setMarkedBy(admin);session.setStatus(AttendanceSessionStatus.FINALIZED);em.persist(session);var record=new AttendanceRecord();record.setSession(session);record.setStudent(s);record.setStatus(AttendanceStatus.LATE);em.persist(record);
  var exam=new Exam();exam.setExamCode("QA-"+tag);exam.setTitle("Parent QA exam");exam.setBatch(batch);exam.setExamType(ExamType.OFFLINE);exam.setEligibilityDate(today.minusDays(2));exam.setStartDateTime(now.minusSeconds(172800));exam.setEndDateTime(now.minusSeconds(169200));exam.setTotalMarks(new BigDecimal("80"));exam.setPassingMarks(new BigDecimal("40"));exam.setCreatedBy(admin);exam.setStatus(ExamStatus.COMPLETED);exam.setResultsPublished(true);exam.setResultsPublishedAt(now);exam.setResultsPublishedBy(admin);em.persist(exam);var result=new ExamResult();result.setExam(exam);result.setStudent(s);result.setObtainedMarks(new BigDecimal("64"));result.setEvaluatedAt(now);result.setEvaluatedBy(admin);em.persist(result);em.flush();
  String base="/api/parents/children/"+s.getId();
  mvc.perform(get(base+"/dashboard").header("Authorization","Bearer "+parent)).andExpect(status().isOk()).andExpect(jsonPath("fees.outstanding").value(600)).andExpect(jsonPath("attendance.attendancePercentage").value(100)).andExpect(jsonPath("progress.averageExamPercentage").value(80));
  mvc.perform(get(base+"/fees").header("Authorization","Bearer "+parent)).andExpect(jsonPath("totals.totalPaid").value(300)).andExpect(jsonPath("totals.overdue").value(600));
  mvc.perform(get(base+"/payments/"+payment+"/receipt").header("Authorization","Bearer "+parent)).andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
  mvc.perform(post("/api/parents/links/"+link+"/revoke").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",verified.get("version").asLong(),"reason","Consent withdrawn")))).andExpect(status().isOk());
  mvc.perform(get(base+"/dashboard").header("Authorization","Bearer "+parent)).andExpect(status().isForbidden());
 }
}
