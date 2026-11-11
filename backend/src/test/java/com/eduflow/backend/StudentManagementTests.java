package com.eduflow.backend;

import com.eduflow.backend.entity.Role;
import com.eduflow.backend.entity.User;
import com.eduflow.backend.repository.StudentRepository;
import com.eduflow.backend.repository.UserRepository;
import com.eduflow.backend.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StudentManagementTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private StudentRepository students;
    @Autowired private UserRepository users;
    @Autowired private JwtService jwt;
    @Autowired private JdbcTemplate jdbc;

    private String adminToken;
    private String studentToken;
    private Long studentUserId;

    @BeforeEach
    void setup() {
        cleanup();
        users.saveAndFlush(new User("Admin", "admin@example.com", "unused", Role.ADMIN));
        User user = users.saveAndFlush(new User("Student", "learner@example.com", "unused", Role.STUDENT));
        studentUserId = user.getId();
        adminToken = jwt.generateToken("admin@example.com");
        studentToken = jwt.generateToken("learner@example.com");
    }

    @AfterEach
    void cleanup() {
        students.deleteAll();
        users.deleteAll();
    }

    private Map<String, Object> request(String email) {
        Map<String, Object> request = new HashMap<>();
        request.put("fullName", "Asha Sharma");
        request.put("email", email);
        request.put("phone", "+919876543210");
        request.put("dateOfBirth", "2005-03-14");
        request.put("address", "24 Park Road, Pune");
        request.put("guardianName", "Meera Sharma");
        request.put("guardianPhone", "9876543211");
        request.put("admissionDate", LocalDate.now().toString());
        return request;
    }

    private JsonNode create(Map<String, Object> body) throws Exception {
        var result = mvc.perform(post("/api/students").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("admissionNumber").isNotEmpty())
                .andExpect(jsonPath("status").value("ACTIVE"))
                .andExpect(jsonPath("createdAt").isNotEmpty())
                .andExpect(jsonPath("updatedAt").isNotEmpty())
                .andExpect(jsonPath("user").doesNotExist())
                .andExpect(jsonPath("password").doesNotExist()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void createsStudentAndGeneratesImmutableUniqueAdmissionNumbers() throws Exception {
        var body = request("first@example.com");
        body.put("admissionNumber", "CLIENT-CONTROLLED");
        JsonNode first = create(body);
        JsonNode second = create(request("second@example.com"));
        assertThat(first.get("admissionNumber").asText()).startsWith("EDU-").isNotEqualTo("CLIENT-CONTROLLED")
                .isNotEqualTo(second.get("admissionNumber").asText());
        mvc.perform(put("/api/students/" + first.get("id").asLong())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("admissionNumber").value(first.get("admissionNumber").asText()));
    }

    @Test
    void databaseRejectsDuplicateAdmissionNumber() throws Exception {
        JsonNode first = create(request("first@example.com"));
        JsonNode second = create(request("second@example.com"));
        assertThatThrownBy(() -> jdbc.update("update students set admission_number = ? where id = ?",
                first.get("admissionNumber").asText(), second.get("id").asLong()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateStudentEmailsButAllowsMatchingUserEmail() throws Exception {
        create(request("learner@example.com"));
        mvc.perform(post("/api/students").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request("LEARNER@example.com"))))
                .andExpect(status().isConflict());
        assertThat(students.count()).isEqualTo(1);
    }

    @Test
    void validatesRequiredFieldsEmailPhoneAndDates() throws Exception {
        var body = request("not-an-email");
        body.put("fullName", " "); body.put("phone", "123"); body.put("guardianPhone", "letters");
        body.put("dateOfBirth", LocalDate.now().plusDays(1).toString());
        body.put("admissionDate", LocalDate.now().plusDays(1).toString());
        body.remove("address"); body.remove("guardianName");
        mvc.perform(post("/api/students").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors.fullName").exists()).andExpect(jsonPath("errors.email").exists())
                .andExpect(jsonPath("errors.phone").exists()).andExpect(jsonPath("errors.guardianPhone").exists())
                .andExpect(jsonPath("errors.address").exists()).andExpect(jsonPath("errors.guardianName").exists())
                .andExpect(jsonPath("errors.dateOfBirth").exists()).andExpect(jsonPath("errors.admissionDate").exists());
        assertThat(students.count()).isZero();
    }

    @Test
    void rejectsAdmissionBeforeBirth() throws Exception {
        var body = request("one@example.com"); body.put("admissionDate", "2000-01-01");
        mvc.perform(post("/api/students").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"STUDENT", "TEACHER", "PARENT"})
    void nonAdminsCannotListCreateUpdateOrDeactivate(Role role) throws Exception {
        User actor = users.saveAndFlush(new User("Other", "other@example.com", "unused", role));
        String token = jwt.generateToken(actor.getEmail());
        var body = request("one@example.com"); body.put("userId", studentUserId);
        JsonNode target = create(request("target@example.com"));
        long id = target.get("id").asLong();
        mvc.perform(get("/api/students").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/students").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/students/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/students/" + id + "/deactivate").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        if (role != Role.STUDENT) {
            mvc.perform(get("/api/students/" + id).header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
        assertThat(students.count()).isEqualTo(1);
    }

    @Test
    void studentCanOnlyReadOwnLinkedProfile() throws Exception {
        var body = request("own@example.com"); body.put("userId", studentUserId);
        JsonNode own = create(body);
        JsonNode other = create(request("other@example.com"));
        mvc.perform(get("/api/students/" + own.get("id").asLong()).header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk()).andExpect(jsonPath("email").value("own@example.com"));
        mvc.perform(get("/api/students/me").param("userId", "9999").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk()).andExpect(jsonPath("id").value(own.get("id").asLong()));
        mvc.perform(get("/api/students/" + other.get("id").asLong()).header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unlinkedStudentHasNoProfileAndAnonymousRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/students/me").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/students")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request("one@example.com"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void linksOnlyStudentAccountsAndPreventsDuplicateLinks() throws Exception {
        var body = request("first@example.com"); body.put("userId", studentUserId);
        create(body);
        body.put("email", "second@example.com");
        mvc.perform(post("/api/students").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
        body.put("userId", users.findByEmail("admin@example.com").orElseThrow().getId());
        mvc.perform(post("/api/students").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        body.put("userId", 999999);
        mvc.perform(post("/api/students").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanUpdateUnlinkAndDeactivateWithoutDeleting() throws Exception {
        var body = request("one@example.com"); body.put("userId", studentUserId);
        JsonNode created = create(body); long id = created.get("id").asLong();
        body.put("fullName", "Updated Student"); body.remove("userId");
        mvc.perform(put("/api/students/" + id).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("fullName").value("Updated Student"))
                .andExpect(jsonPath("userId").isEmpty());
        mvc.perform(get("/api/students/me").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/students/" + id + "/deactivate").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("status").value("INACTIVE"));
        mvc.perform(patch("/api/students/" + id + "/deactivate").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        assertThat(students.count()).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateEmailOnUpdateWithoutChangingExistingData() throws Exception {
        create(request("one@example.com"));
        JsonNode second = create(request("two@example.com"));
        mvc.perform(put("/api/students/" + second.get("id").asLong()).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request("one@example.com"))))
                .andExpect(status().isConflict());
        assertThat(students.findById(second.get("id").asLong()).orElseThrow().getEmail()).isEqualTo("two@example.com");
    }

    @Test
    void searchesPaginatesFiltersAndTreatsWildcardsLiterally() throws Exception {
        JsonNode first = create(request("one@example.com")); create(request("two@example.com"));
        mvc.perform(get("/api/students").param("size", "1").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(2))
                .andExpect(jsonPath("totalPages").value(2)).andExpect(jsonPath("content.length()").value(1));
        mvc.perform(get("/api/students").param("page", "1").param("size", "1").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("page").value(1));
        mvc.perform(get("/api/students").param("search", "ONE@EXAMPLE").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1));
        mvc.perform(get("/api/students").param("search", "%").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(0));
        mvc.perform(patch("/api/students/" + first.get("id").asLong() + "/deactivate").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/students").param("status", "INACTIVE").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1));
    }

    @Test
    void missingRecordsAndInvalidParametersReturnProperErrors() throws Exception {
        mvc.perform(get("/api/students/999999").header("Authorization", "Bearer " + adminToken)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/students/999999/deactivate").header("Authorization", "Bearer " + adminToken)).andExpect(status().isNotFound());
        for (String query : new String[]{"size=101", "page=-1", "size=0", "page=bad", "status=UNKNOWN"}) {
            mvc.perform(get("/api/students?" + query).header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest());
        }
    }
}
