package com.eduflow.backend;

import com.eduflow.backend.entity.PasswordResetToken;
import com.eduflow.backend.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdministrationTests extends NotificationManagementTests {
    @Test void readinessRequiresAdministratorAndOnlyReturnsStatus() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/health/readiness")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/health/readiness").header("Authorization", "Bearer " + student)).andExpect(status().isForbidden());
        mvc.perform(get("/api/health/readiness").header("Authorization", "Bearer " + admin)).andExpect(status().isOk()).andExpect(content().json("{\"status\":\"UP\"}"));
    }
    @Test void unknownRoutesMissingFilesAndCorruptImagesUseSafeErrors() throws Exception {
        mvc.perform(get("/api/not-a-real-endpoint").header("Authorization","Bearer "+admin)).andExpect(status().isNotFound()).andExpect(jsonPath("requestId").exists());
        mvc.perform(multipart("/api/admin/institute-settings/logo").header("Authorization","Bearer "+admin)).andExpect(status().isBadRequest());
        byte[] damaged={(byte)137,80,78,71,13,10,26,10,1};
        mvc.perform(multipart("/api/admin/institute-settings/logo").file(new MockMultipartFile("file","broken.png","image/png",damaged)).header("Authorization","Bearer "+admin)).andExpect(status().isBadRequest());
    }
    @Autowired
    PasswordEncoder encoder;
    @Autowired
    AuditLogRepository audits;

    @BeforeEach
    void passwords() {
        for (var u : users.findAll()) {
            u.setPassword(encoder.encode("CurrentPass123!"));
        }
        em.flush();
    }

    Map<String, Object> settings() throws Exception {
        return mapper.convertValue(mapper.readTree(mvc.perform(get("/api/admin/institute-settings").header("Authorization", "Bearer " + admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()), Map.class);
    }

    @Test
    void settingsValidationAuthorizationAndRealBranding() throws Exception {
        var input = settings();
        input.put("instituteName", "Verified Institute");
        mvc.perform(json(put("/api/admin/institute-settings"), input, admin)).andExpect(status().isOk()).andExpect(jsonPath("instituteName").value("Verified Institute"));
        mvc.perform(get("/api/institute/branding").header("Authorization", "Bearer " + student)).andExpect(jsonPath("instituteName").value("Verified Institute"));
        var invalid = settings();
        invalid.put("timezone", "Invalid/Zone");
        mvc.perform(json(put("/api/admin/institute-settings"), invalid, admin)).andExpect(status().isBadRequest());
        mvc.perform(json(put("/api/admin/institute-settings"), invalid, teacher)).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/admin/institute-settings/logo").file(new MockMultipartFile("file", "bad.png", "image/png", "not an image".getBytes())).header("Authorization", "Bearer " + admin)).andExpect(status().isBadRequest());
    }

    @Test
    void validLogoUsesPrivateStorage() throws Exception {
        var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", out);
        mvc.perform(multipart("/api/admin/institute-settings/logo").file(new MockMultipartFile("file", "logo.png", "image/png", out.toByteArray())).header("Authorization", "Bearer " + admin)).andExpect(status().isOk()).andExpect(jsonPath("hasLogo").value(true));
        mvc.perform(get("/api/institute/logo").header("Authorization", "Bearer " + student)).andExpect(status().isOk()).andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void deactivationAndUnlockRevokeExistingTokens() throws Exception {
        mvc.perform(json(patch("/api/admin/users/" + studentUser.getId() + "/status"), Map.of("status", "LOCKED"), admin)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + student)).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/auth/login"), Map.of("email", studentUser.getEmail(), "password", "CurrentPass123!"), null)).andExpect(status().isUnauthorized());
        mvc.perform(json(patch("/api/admin/users/" + studentUser.getId() + "/status"), Map.of("status", "ACTIVE"), admin)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + student)).andExpect(status().isUnauthorized());
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("email", studentUser.getEmail(), "password", "CurrentPass123!")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + mapper.readTree(login).get("token").asText())).andExpect(status().isOk());
    }

    @Test
    void selfLockoutRoleLinkAndPrivilegeEscalationPrevented() throws Exception {
        mvc.perform(json(patch("/api/admin/users/" + adminUser.getId() + "/status"), Map.of("status", "INACTIVE"), admin)).andExpect(status().isConflict());
        mvc.perform(json(patch("/api/admin/users/" + adminUser.getId() + "/roles"), Map.of("role", "STUDENT"), admin)).andExpect(status().isConflict());
        mvc.perform(json(patch("/api/admin/users/" + studentUser.getId() + "/roles"), Map.of("role", "ADMIN"), student)).andExpect(status().isForbidden());
        mvc.perform(json(patch("/api/admin/users/" + teacherUser.getId() + "/roles"), Map.of("role", "PARENT"), admin)).andExpect(status().isConflict());
    }

    @Test
    void passwordChangeVerifiesCurrentAndRevokesAllTokens() throws Exception {
        mvc.perform(json(put("/api/settings/security/password"), Map.of("currentPassword", "wrong", "newPassword", "ChangedPass123!"), student)).andExpect(status().isBadRequest());
        mvc.perform(json(put("/api/settings/security/password"), Map.of("currentPassword", "CurrentPass123!", "newPassword", "ChangedPass123!"), student)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + student)).andExpect(status().isUnauthorized());
        assertThat(encoder.matches("ChangedPass123!", studentUser.getPassword())).isTrue();
    }

    String recovery() throws Exception {
        return mapper.readTree(mvc.perform(post("/api/admin/users/" + studentUser.getId() + "/password-reset").header("Authorization", "Bearer " + admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("resetToken").asText();
    }

    @Test
    void resetTokensAreHashedSingleUseAndNeverEnumerated() throws Exception {
        String t = recovery();
        assertThat(em.createQuery("select t.tokenHash from PasswordResetToken t", String.class).getSingleResult()).doesNotContain(t);
        for (String email : List.of("missing@example.invalid", studentUser.getEmail()))
            mvc.perform(post("/api/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("email", email)))).andExpect(status().isOk()).andExpect(jsonPath("resetToken").doesNotExist());
        String input = mapper.writeValueAsString(Map.of("token", t, "newPassword", "RecoveredPass123!"));
        mvc.perform(post("/api/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + student)).andExpect(status().isUnauthorized());
        assertThat(audits.findAll().stream().noneMatch(a -> a.description.contains(t))).isTrue();
    }

    @Test
    void expiredResetRejectedAndRequestsRateLimited() throws Exception {
        String t = recovery();
        var row = em.createQuery("select t from PasswordResetToken t", PasswordResetToken.class).getSingleResult();
        row.expiresAt = Instant.now().minusSeconds(1);
        em.flush();
        mvc.perform(post("/api/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("token", t, "newPassword", "RecoveredPass123!")))).andExpect(status().isBadRequest());
        for (int i = 0; i < 3; i++)
            mvc.perform(post("/api/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("email", studentUser.getEmail())))).andExpect(status().isOk());
        assertThat(em.createQuery("select count(t) from RecoveryThrottle t", Long.class).getSingleResult()).isEqualTo(1);
    }

    @Test
    void auditAndControlCenterAreAdminOnlyWithSafeDtos() throws Exception {
        mvc.perform(json(put("/api/settings/profile"), Map.of("fullName", "Updated Student"), student)).andExpect(status().isOk()).andExpect(jsonPath("password").doesNotExist());
        mvc.perform(get("/api/admin/control-center").header("Authorization", "Bearer " + admin)).andExpect(status().isOk()).andExpect(jsonPath("counts.activeUsers").isNumber());
        mvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer " + admin)).andExpect(status().isOk()).andExpect(jsonPath("totalElements").isNumber());
        mvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer " + parent)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + admin)).andExpect(status().isOk()).andExpect(jsonPath("content[0].password").doesNotExist());
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized()).andExpect(jsonPath("timestamp").exists()).andExpect(jsonPath("requestId").exists()).andExpect(jsonPath("path").value("/api/admin/users"));
    }
}
