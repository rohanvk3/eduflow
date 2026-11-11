package com.eduflow.backend.controller;

import com.eduflow.backend.dto.AdminDtos.*;
import com.eduflow.backend.dto.PageResponse;
import com.eduflow.backend.entity.AccountStatus;
import com.eduflow.backend.entity.Role;
import com.eduflow.backend.service.AccountSettingsService;
import com.eduflow.backend.service.AdministrationService;
import com.eduflow.backend.service.InstituteSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdministrationController {
    final AdministrationService service;
    final InstituteSettingsService institute;
    final AccountSettingsService accounts;

    public AdministrationController(AdministrationService service, InstituteSettingsService institute, AccountSettingsService accounts) {
        this.service = service;
        this.institute = institute;
        this.accounts = accounts;
    }

    @GetMapping("/control-center")
    public Map<String, Object> center() {
        return service.center();
    }

    @GetMapping("/institute-settings")
    public Settings settings() {
        return institute.get();
    }

    @PutMapping("/institute-settings")
    public Settings settings(@Valid @RequestBody Settings input) {
        return institute.update(input);
    }

    @PostMapping("/institute-settings/logo")
    public Settings logo(@RequestParam MultipartFile file) throws IOException {
        return institute.logo(file);
    }

    @GetMapping("/users")
    public PageResponse<Account> users(@RequestParam(defaultValue = "") String search, @RequestParam(required = false) Role role, @RequestParam(required = false) AccountStatus status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.users(search, role, status, page, size);
    }

    @GetMapping("/users/{id}")
    public Account user(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/users/{id}/status")
    public Account status(@PathVariable Long id, @Valid @RequestBody StatusRequest input) {
        return service.status(id, input.status());
    }

    @PatchMapping("/users/{id}/roles")
    public Account role(@PathVariable Long id, @Valid @RequestBody RoleRequest input) {
        return service.role(id, input.role());
    }

    @PostMapping("/users/{id}/password-reset")
    public RecoveryLink reset(@PathVariable Long id) {
        return accounts.issue(id);
    }

    @GetMapping("/audit-logs")
    public PageResponse<AuditView> logs(@RequestParam(defaultValue = "") String search, @RequestParam(required = false) String action, @RequestParam(required = false) Long actorUserId, @RequestParam(required = false) String entityType, @RequestParam(required = false) String outcome, @RequestParam(required = false) LocalDate fromDate, @RequestParam(required = false) LocalDate toDate, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.logs(search, action, actorUserId, entityType, outcome, fromDate, toDate, page, size);
    }

    @GetMapping("/audit-logs/{id}")
    public AuditView log(@PathVariable Long id) {
        return service.log(id);
    }
}
