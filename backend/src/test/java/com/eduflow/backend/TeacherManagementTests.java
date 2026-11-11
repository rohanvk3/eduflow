package com.eduflow.backend;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.repository.*;
import com.eduflow.backend.security.JwtService;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class TeacherManagementTests {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper mapper;
 @Autowired TeacherRepository teachers;
 @Autowired StudentRepository students;
 @Autowired UserRepository users;
 @Autowired JwtService jwt;
 String admin; String teacher; String student; Long teacherUserId;
 @BeforeEach void setup() {
  clean();
  users.saveAndFlush(new User("Admin", "admin@example.com", "unused", Role.ADMIN));
  teacherUserId = users.saveAndFlush(new User("Teacher", "teacher@example.com", "unused", Role.TEACHER)).getId();
  users.saveAndFlush(new User("Student", "student@example.com", "unused", Role.STUDENT));
  admin = jwt.generateToken("admin@example.com"); teacher = jwt.generateToken("teacher@example.com"); student = jwt.generateToken("student@example.com");
 }
 @AfterEach void clean() { teachers.deleteAll(); students.deleteAll(); users.deleteAll(); }
 Map<String,Object> body(String employee, String email) {
  return new HashMap<>(Map.of("employeeId", employee, "email", email, "fullName", "Faculty Member", "specialization", "Mathematics", "subjects", List.of("Math", "Algebra"), "salary", "50000.25", "experienceYears", 5));
 }
 JsonNode create(Map<String,Object> body) throws Exception {
  return mapper.readTree(mvc.perform(post("/api/teachers").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body))).andExpect(status().isCreated()).andExpect(header().exists("Location")).andExpect(jsonPath("status").value("ACTIVE")).andReturn().getResponse().getContentAsString());
 }
 @Test void createGetUpdateAndPreserveOptionalFields() throws Exception {
  long id = create(body("EMP-1", "faculty@example.com")).get("id").asLong();
  mvc.perform(get("/api/teachers/" + id).header("Authorization","Bearer " + admin)).andExpect(status().isOk()).andExpect(jsonPath("salary").value(50000.25));
  mvc.perform(put("/api/teachers/" + id).header("Authorization","Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":\"EMP-1\",\"email\":\"faculty@example.com\",\"fullName\":\"Updated\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("fullName").value("Updated")).andExpect(jsonPath("salary").value(50000.25)).andExpect(jsonPath("subjects.length()").value(2));
 }
 @Test void duplicateEmployeeAndEmailReturn409() throws Exception {
  create(body("EMP-1","one@example.com"));
  for (var b : List.of(body("emp-1","two@example.com"),body("EMP-2","ONE@example.com")))
   mvc.perform(post("/api/teachers").header("Authorization","Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(b))).andExpect(status().isConflict());
 }
 @Test void validatesInputs() throws Exception {
  var b = body("bad id!","invalid"); b.put("fullName"," "); b.put("salary",-1); b.put("experienceYears",-1); b.put("joiningDate","2999-01-01"); b.put("phone","123");
  mvc.perform(post("/api/teachers").header("Authorization","Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(b))).andExpect(status().isBadRequest()).andExpect(jsonPath("errors.salary").exists()).andExpect(jsonPath("errors.employeeId").exists()).andExpect(jsonPath("errors.joiningDate").exists());
 }
 @Test void searchPaginationSortAndStats() throws Exception {
  create(body("EMP-1","one@example.com")); create(body("EMP-2","two@example.com"));
  mvc.perform(get("/api/teachers").param("search","MATH").param("size","1").param("sort","employeeId,asc").header("Authorization","Bearer " + admin)).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(2)).andExpect(jsonPath("content.length()").value(1));
  mvc.perform(get("/api/teachers/stats").header("Authorization","Bearer " + admin)).andExpect(status().isOk()).andExpect(jsonPath("active").value(2));
  mvc.perform(get("/api/teachers").param("sort","salary,desc").header("Authorization","Bearer " + admin)).andExpect(status().isBadRequest());
 }
 @Test void deactivateAndReactivateRetainsRecord() throws Exception {
  long id = create(body("EMP-1","one@example.com")).get("id").asLong();
  for (var operation : List.of("deactivate","activate")) {
   mvc.perform(patch("/api/teachers/" + id + "/" + operation).header("Authorization","Bearer " + admin)).andExpect(status().isOk()).andExpect(jsonPath("status").value(operation.equals("activate") ? "ACTIVE" : "INACTIVE"));
  }
  assertThat(teachers.count()).isEqualTo(1);
 }
 @Test void ownProfileNeverIncludesSalaryAndCannotReadOtherRecords() throws Exception {
  var b = body("EMP-1","contact@example.com"); b.put("userId",teacherUserId); create(b);
  long other = create(body("EMP-2","other@example.com")).get("id").asLong();
  mvc.perform(get("/api/teachers/me").param("userId","999").header("Authorization","Bearer " + teacher)).andExpect(status().isOk()).andExpect(jsonPath("employeeId").value("EMP-1")).andExpect(jsonPath("salary").doesNotExist()).andExpect(jsonPath("userId").doesNotExist());
  mvc.perform(get("/api/teachers/" + other).header("Authorization","Bearer " + teacher)).andExpect(status().isForbidden());
  b.put("email","new@example.com"); b.put("employeeId","EMP-3");
  mvc.perform(post("/api/teachers").header("Authorization","Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(b))).andExpect(status().isConflict());
 }
 @ParameterizedTest @ValueSource(strings={"/api/teachers","/api/teachers/stats","/api/teachers/1"})
 void adminReadsDeniedToOtherRoles(String path) throws Exception {
  for (var token : List.of(teacher,student)) mvc.perform(get(path).header("Authorization","Bearer " + token)).andExpect(status().isForbidden());
  mvc.perform(get(path)).andExpect(status().isUnauthorized());
 }
 @Test void nonAdminWritesDenied() throws Exception {
  for (var token : List.of(teacher,student)) {
   mvc.perform(post("/api/teachers").header("Authorization","Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body("EMP-1","one@example.com")))).andExpect(status().isForbidden());
   mvc.perform(put("/api/teachers/1").header("Authorization","Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body("EMP-1","one@example.com")))).andExpect(status().isForbidden());
   mvc.perform(patch("/api/teachers/1/activate").header("Authorization","Bearer " + token)).andExpect(status().isForbidden());
   mvc.perform(patch("/api/teachers/1/deactivate").header("Authorization","Bearer " + token)).andExpect(status().isForbidden());
  }
 }
 @Test void linksRequireTeacherRoleAndMissingProfileIs404() throws Exception {
  mvc.perform(get("/api/teachers/me").header("Authorization","Bearer " + teacher)).andExpect(status().isNotFound());
  mvc.perform(get("/api/teachers/me").header("Authorization","Bearer " + student)).andExpect(status().isForbidden());
  var b = body("EMP-1","one@example.com"); b.put("userId",users.findByEmail("student@example.com").orElseThrow().getId());
  mvc.perform(post("/api/teachers").header("Authorization","Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(b))).andExpect(status().isBadRequest());
  assertThat(users.findByEmail("student@example.com").orElseThrow().getRole()).isEqualTo(Role.STUDENT);
  mvc.perform(get("/api/teachers/99999").header("Authorization","Bearer " + admin)).andExpect(status().isNotFound());
 }
}
