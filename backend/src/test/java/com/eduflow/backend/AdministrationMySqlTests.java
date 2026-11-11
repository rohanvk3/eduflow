package com.eduflow.backend;

import com.eduflow.backend.entity.Role;
import com.eduflow.backend.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfEnvironmentVariable(named = "EDUFLOW_MYSQL_TEST", matches = "true")
@org.springframework.test.context.ContextConfiguration(initializers = IsolatedMySqlInitializer.class)
@SpringBootTest(properties = {"spring.datasource.url=${EDUFLOW_TEST_DB_URL}", "spring.datasource.username=${EDUFLOW_TEST_DB_USERNAME}", "spring.datasource.password=${EDUFLOW_TEST_DB_PASSWORD}", "spring.jpa.hibernate.ddl-auto=update", "app.jwt.secret=dGVzdC1vbmx5LXNlY3JldC1rZXktMzItYnl0ZXMtbG9uZyE="})
class AdministrationMySqlTests extends AcademicMySqlTests {
    @Test
    void mysqlSettingsStatusRevocationRecoveryAndAudit() throws Exception {
        var a = users.saveAndFlush(new User("Admin QA", UUID.randomUUID() + "@qa.invalid", "unused", Role.ADMIN));
        token = jwt.generateToken(a.getEmail());
        var s = users.saveAndFlush(new User("Student QA", UUID.randomUUID() + "@qa.invalid", "unused", Role.STUDENT));
        String old = jwt.generateToken(s.getEmail());
        var settings = mapper.readTree(mvc.perform(get("/api/admin/institute-settings").header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var input = mapper.convertValue(settings, Map.class);
        input.put("instituteName", "Verified QA Institute");
        mvc.perform(put("/api/admin/institute-settings").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(input))).andExpect(status().isOk());
        mvc.perform(get("/api/admin/control-center").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(patch("/api/admin/users/" + s.getId() + "/status").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + old)).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/admin/users/" + s.getId() + "/status").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}")).andExpect(status().isOk());
        String reset = mapper.readTree(mvc.perform(post("/api/admin/users/" + s.getId() + "/password-reset").header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("resetToken").asText();
        mvc.perform(post("/api/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("token", reset, "newPassword", "MySqlRecovered123!")))).andExpect(status().isNoContent());
        var pdf = mvc.perform(get("/api/reports/export/pdf").param("reportType", "REVENUE").header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (var doc = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(doc)).contains("Verified QA Institute");
        }
        mvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andExpect(jsonPath("totalElements").isNumber());
    }
}
