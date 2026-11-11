package com.eduflow.backend.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final javax.sql.DataSource dataSource;

    public HealthController(javax.sql.DataSource dataSource) { this.dataSource = dataSource; }

    @GetMapping("/api/health/readiness")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public org.springframework.http.ResponseEntity<Map<String, String>> readiness() {
        try (var connection = dataSource.getConnection()) {
            if (connection.isValid(2)) return org.springframework.http.ResponseEntity.ok(Map.of("status", "UP"));
        } catch (java.sql.SQLException exception) {
            // Do not disclose connection details or credentials.
        }
        return org.springframework.http.ResponseEntity.status(503).body(Map.of("status", "DOWN"));
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
