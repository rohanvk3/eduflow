package com.eduflow.backend.controller;

import com.eduflow.backend.dto.NotificationDtos.AnnouncementRequest;
import com.eduflow.backend.dto.NotificationDtos.AnnouncementView;
import com.eduflow.backend.dto.NotificationDtos.DeliveryOverview;
import com.eduflow.backend.dto.NotificationDtos.VersionRequest;
import com.eduflow.backend.dto.PageResponse;
import com.eduflow.backend.entity.AnnouncementAudience;
import com.eduflow.backend.entity.AnnouncementStatus;
import com.eduflow.backend.service.AnnouncementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    final AnnouncementService service;

    public AnnouncementController(AnnouncementService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AnnouncementView> create(@Valid @RequestBody AnnouncementRequest input) {
        var a = service.save(null, input);
        return ResponseEntity.created(URI.create("/api/announcements/" + a.id())).body(a);
    }

    @GetMapping
    public PageResponse<AnnouncementView> list(@RequestParam(defaultValue = "") String search, @RequestParam(required = false) AnnouncementAudience audienceType, @RequestParam(required = false) AnnouncementStatus status, @RequestParam(required = false) Long courseId, @RequestParam(required = false) Long batchId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt,desc") String sort, @RequestParam(defaultValue = "false") boolean createdByMe) {
        return service.list(search, audienceType, status, courseId, batchId, page, size, sort, false, createdByMe);
    }

    @GetMapping("/me")
    public PageResponse<AnnouncementView> mine(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return service.list("", null, null, null, null, page, size, "publishedAt,desc", true);
    }

    @GetMapping("/overview")
    public DeliveryOverview overview() {
        return service.overview();
    }

    @GetMapping("/{id}")
    public AnnouncementView get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public AnnouncementView edit(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest input) {
        return service.save(id, input);
    }

    @PostMapping("/{id}/publish")
    public AnnouncementView publish(@PathVariable Long id, @Valid @RequestBody VersionRequest input) {
        return service.publish(id, input);
    }

    @PostMapping("/{id}/archive")
    public AnnouncementView archive(@PathVariable Long id, @Valid @RequestBody VersionRequest input) {
        return service.archive(id, input);
    }
}
