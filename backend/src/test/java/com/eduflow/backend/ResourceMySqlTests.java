package com.eduflow.backend;
import com.eduflow.backend.entity.*;
import java.time.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;
import org.apache.pdfbox.pdmodel.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@EnabledIfEnvironmentVariable(named="EDUFLOW_MYSQL_TEST",matches="true")
@org.springframework.test.context.ContextConfiguration(initializers = IsolatedMySqlInitializer.class)
@SpringBootTest(properties={"spring.datasource.url=${EDUFLOW_TEST_DB_URL}","spring.datasource.username=${EDUFLOW_TEST_DB_USERNAME}","spring.datasource.password=${EDUFLOW_TEST_DB_PASSWORD}","spring.jpa.hibernate.ddl-auto=update","app.jwt.secret=dGVzdC1vbmx5LXNlY3JldC1rZXktMzItYnl0ZXMtbG9uZyE=","app.exams.expiry-enabled=false"})
class ResourceMySqlTests extends AcademicMySqlTests {
 static final Path storage;static{try{storage=Files.createTempDirectory("eduflow-resource-mysql-");}catch(IOException e){throw new ExceptionInInitializerError(e);}}
 @DynamicPropertySource static void storage(DynamicPropertyRegistry p){p.add("app.assignments.storage-dir",()->storage.toString());}
 @AfterTransaction void filesRolledBack()throws Exception{try(var entries=Files.list(storage)){assertThat(entries.count()).isZero();}}
 long version(long id)throws Exception{return mapper.readTree(mvc.perform(get("/api/resources/materials/"+id).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("version").asLong();}
 @Test void mysqlResourcePublicationVersionsPrivateDownloadsAndStatistics()throws Exception{
  String tag=UUID.randomUUID().toString().replace("-","");var admin=users.saveAndFlush(new User("Resource QA",tag+"@admin.invalid","unused",Role.ADMIN));token=jwt.generateToken(admin.getEmail());LocalDate today=LocalDate.now(ZoneId.of("Asia/Kolkata"));
  var login=users.saveAndFlush(new User("Resource learner",tag+"@login.invalid","unused",Role.STUDENT));String learner=jwt.generateToken(login.getEmail());
  var student=new Student("QA-"+tag);student.setFullName("Resource learner");student.setEmail(tag+"@student.invalid");student.setPhone("9876543210");student.setDateOfBirth(today.minusYears(18));student.setAdmissionDate(today.minusDays(5));student.setGuardianName("Guardian");student.setGuardianPhone("9876543211");student.setAddress("QA");student.setUser(login);students.saveAndFlush(student);
  long course=create("/api/courses",Map.of("code","QA-"+tag,"name","Resource QA","durationWeeks",12));
  long batch=create("/api/batches",Map.of("code","QA-"+tag,"name","Resource QA","courseId",course,"capacity",5,"startDate",today.minusDays(3).toString(),"endDate",today.plusMonths(1).toString(),"status","ACTIVE"));
  create("/api/batches/"+batch+"/enrollments",Map.of("studentId",student.getId(),"enrollmentDate",today.minusDays(1).toString()));
  long category=create("/api/resources/categories",Map.of("name","QA "+tag));
  long folder=create("/api/resources/folders",Map.of("name","QA folder","courseId",course,"batchId",batch));
  var metadata=Map.of("title","QA "+tag,"courseId",course,"batchId",batch,"categoryId",category,"folderId",folder,"resourceType","DOCUMENT");
  byte[] pdf;try(var document=new PDDocument();var out=new ByteArrayOutputStream()){document.addPage(new PDPage());document.save(out);pdf=out.toByteArray();}
  long id=mapper.readTree(mvc.perform(multipart("/api/resources/materials").file(new MockMultipartFile("data","data","application/json",mapper.writeValueAsBytes(metadata))).file(new MockMultipartFile("file","notes.pdf","application/pdf",pdf)).header("Authorization","Bearer "+token)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
  mvc.perform(get("/api/resources/materials/"+id).header("Authorization","Bearer "+learner)).andExpect(status().isForbidden());
  mvc.perform(post("/api/resources/materials/"+id+"/publish").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",version(id))))).andExpect(status().isOk());
  mvc.perform(get("/api/resources/materials").param("folderId",Long.toString(folder)).header("Authorization","Bearer "+learner)).andExpect(jsonPath("totalElements").value(1));
  mvc.perform(multipart("/api/resources/materials/"+id+"/versions").file(new MockMultipartFile("file","replacement.txt","text/plain","Updated notes".getBytes())).param("version",Long.toString(version(id))).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("currentVersion").value(2));
  mvc.perform(get("/api/resources/materials/"+id+"/download").header("Authorization","Bearer "+learner)).andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
  mvc.perform(get("/api/resources/stats").header("Authorization","Bearer "+learner)).andExpect(status().isOk()).andExpect(jsonPath("totalMaterials").value(1)).andExpect(jsonPath("courses[0].count").value(1));
  mvc.perform(post("/api/resources/materials/"+id+"/publish").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",version(id))))).andExpect(status().isOk());
  mvc.perform(get("/api/resources/materials/"+id+"/download").header("Authorization","Bearer "+learner)).andExpect(status().isOk()).andExpect(content().contentType("text/plain"));
  mvc.perform(delete("/api/resources/folders/"+folder).header("Authorization","Bearer "+token)).andExpect(status().isConflict());
 }
}
