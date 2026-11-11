package com.eduflow.backend.controller;

import com.eduflow.backend.dto.AdminDtos.*;
import com.eduflow.backend.service.AccountSettingsService;
import com.eduflow.backend.service.InstituteSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AccountSettingsController {
    final AccountSettingsService service;
    final InstituteSettingsService institute;

    public AccountSettingsController(AccountSettingsService service, InstituteSettingsService institute) {
        this.service = service;
        this.institute = institute;
    }

    @GetMapping("/api/settings/profile")
    public Account profile() {
        return service.profile();
    }

    @PutMapping("/api/settings/profile")
    public Account profile(@Valid @RequestBody ProfileRequest input) {
        return service.profile(input);
    }

    @PutMapping("/api/settings/security/password")
    public ResponseEntity<Void> password(@Valid @RequestBody PasswordChange input) {
        service.password(input);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/institute/branding")
    public Branding branding() {
        return institute.branding();
    }

    @GetMapping("/api/institute/logo")
    public ResponseEntity<byte[]> logo() {
        return institute.logo();
    }

    @PostMapping("/api/auth/password-reset/request")
    public Map<String, String> request(@Valid @RequestBody ResetRequest input, HttpServletRequest request) {
        service.requestReset(input, request.getRemoteAddr());
        return Map.of("message", "If this account exists, institute administrators can review the recovery request. Contact the institute for verified assistance. No email is sent by this installation.");
    }

    @PostMapping("/api/auth/password-reset/confirm")
    public ResponseEntity<Void> confirm(@Valid @RequestBody ResetConfirm input) {
        service.confirm(input);
        return ResponseEntity.noContent().build();
    }
}
