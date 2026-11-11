package com.eduflow.backend;

import com.eduflow.backend.entity.Role;
import com.eduflow.backend.entity.User;
import com.eduflow.backend.repository.UserRepository;
import com.eduflow.backend.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(EduFlowApplicationTests.ProtectedTestController.class)
class EduFlowApplicationTests {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtService jwt;
    @Autowired private ObjectMapper mapper;
    @Value("${app.jwt.secret}") private String secret;

    private static final String REGISTRATION = """
            {"fullName":"Test Student","email":"student@example.com","password":"Password123!"}
            """;

    @BeforeEach
    void clearUsers() {
        users.deleteAll();
    }

    @Test
    void healthIsPublicAndReturnsUp() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}", true));
    }

    @Test
    void registrationPersistsHashedPasswordAndReturnsSafeDetails() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTRATION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id").isNumber())
                .andExpect(jsonPath("fullName").value("Test Student"))
                .andExpect(jsonPath("role").value("STUDENT"))
                .andExpect(jsonPath("createdAt").isNotEmpty())
                .andExpect(jsonPath("password").doesNotExist());
        User user = users.findByEmail("student@example.com").orElseThrow();
        assertThat(user.getPassword()).startsWith("$2");
        assertThat(encoder.matches("Password123!", user.getPassword())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "TEACHER", "PARENT"})
    void publicRegistrationCannotChoosePrivilegedRole(String role) throws Exception {
        String body = REGISTRATION.strip().replace("}", ",\"role\":\"" + role + "\"}");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("role").value("STUDENT"));
        assertThat(users.findByEmail("student@example.com").orElseThrow().getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void registrationValidatesInputsWithoutEchoingPassword() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\" \",\"email\":\"invalid\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors.fullName").exists())
                .andExpect(jsonPath("errors.email").exists())
                .andExpect(jsonPath("errors.password").exists());
        assertThat(users.count()).isZero();
    }

    @Test
    void bcryptByteLimitIsValidated() throws Exception {
        String body = mapper.writeValueAsString(java.util.Map.of("fullName", "Student",
                "email", "student@example.com", "password", "é".repeat(40)));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        seed(Role.STUDENT);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRATION.replace("student@example.com", "STUDENT@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("message").value("Email is already registered"));
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void loginReturnsUsableTokenAndMeReturnsCurrentUser() throws Exception {
        User user = seed(Role.STUDENT);
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"STUDENT@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("token").isNotEmpty())
                .andExpect(jsonPath("userId").value(user.getId()))
                .andExpect(jsonPath("fullName").value("Test Student"))
                .andExpect(jsonPath("email").value("student@example.com"))
                .andExpect(jsonPath("role").value("STUDENT"))
                .andExpect(jsonPath("password").doesNotExist())
                .andExpect(cookie().doesNotExist("JSESSIONID"))
                .andReturn();
        String token = mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value(user.getId()))
                .andExpect(jsonPath("email").value("student@example.com"))
                .andExpect(jsonPath("password").doesNotExist())
                .andExpect(cookie().doesNotExist("JSESSIONID"));
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"student@example.com", "missing@example.com"})
    void invalidCredentialsReturnSameError(String email) throws Exception {
        seed(Role.STUDENT);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"incorrect\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Invalid email or password"));
    }

    @Test
    void malformedJsonAndMissingLoginFieldsReturn400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void otherEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/test-admin")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/unknown")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/health")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/login")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void rolesControlMethodAccess(Role role) throws Exception {
        seed(role);
        String token = jwt.generateToken("student@example.com");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("role").value(role.name()));
        mvc.perform(get("/api/test-admin").header("Authorization", "Bearer " + token))
                .andExpect(status().is(role == Role.ADMIN ? 200 : 403));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-jwt", "", "a.b.c"})
    void malformedTokensReturn401(String token) throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("message").value("Invalid JWT"));
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        seed(Role.STUDENT);
        String token = Jwts.builder().issuer("eduflow").subject("student@example.com")
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)), Jwts.SIG.HS256).compact();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("message").value("JWT has expired"));
    }

    @Test
    void wrongSignatureAndMissingExpirationAreRejected() throws Exception {
        seed(Role.STUDENT);
        String wrongSignature = Jwts.builder().issuer("eduflow").subject("student@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Jwts.SIG.HS256.key().build()).compact();
        String missingExpiration = Jwts.builder().issuer("eduflow").subject("student@example.com")
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))).compact();
        for (String token : new String[]{wrongSignature, missingExpiration}) {
            mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void tokenForDeletedUserIsRejected() throws Exception {
        String token = jwt.generateToken("deleted@example.com");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void frontendCorsAndPreflightAreAllowed() throws Exception {
        mvc.perform(get("/api/health").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/auth/me").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void otherOriginsAreRejected() throws Exception {
        mvc.perform(options("/api/auth/me").header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    private User seed(Role role) {
        return users.saveAndFlush(new User("Test Student", "student@example.com", encoder.encode("Password123!"), role));
    }

    @RestController
    static class ProtectedTestController {
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/api/test-admin")
        public String adminOnly() {
            return "admin";
        }
    }
}
