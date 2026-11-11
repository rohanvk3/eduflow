package com.eduflow.backend.controller;

import com.eduflow.backend.dto.NotificationDtos.NotificationView;
import com.eduflow.backend.dto.NotificationDtos.PreferenceRequest;
import com.eduflow.backend.dto.NotificationDtos.Preferences;
import com.eduflow.backend.dto.PageResponse;
import com.eduflow.backend.entity.NotificationType;
import com.eduflow.backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {
    final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<NotificationView> list(@RequestParam(required = false) NotificationType type, @RequestParam(defaultValue = "ALL") String readStatus, @RequestParam(required = false) LocalDate fromDate, @RequestParam(required = false) LocalDate toDate, @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return service.list(type, readStatus, fromDate, toDate, search, page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> count() {
        return Map.of("count", service.unread());
    }

    @PatchMapping("/{id}/read")
    public NotificationView read(@PathVariable Long id) {
        return service.read(id);
    }

    @PatchMapping("/read-all")
    public Map<String, Long> all() {
        return Map.of("updated", service.readAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> dismiss(@PathVariable Long id) {
        service.dismiss(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public Preferences preferences() {
        return service.preferences();
    }

    @PutMapping("/preferences")
    public Preferences update(@jakarta.validation.Valid @RequestBody PreferenceRequest input) {
        return service.updatePreferences(input);
    }
}
