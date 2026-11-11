package com.eduflow.backend.controller;

import com.eduflow.backend.dto.AnalyticsDtos.Data;
import com.eduflow.backend.dto.AnalyticsDtos.Filter;
import com.eduflow.backend.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public Data overview(@ModelAttribute Filter f) {
        return service.overview(f);
    }

    @GetMapping("/finance/{section}")
    public Data finance(@PathVariable String section, @ModelAttribute Filter f, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        if (!Set.of("summary", "revenue-trend", "outstanding", "payment-methods", "course-wise", "batch-wise", "payments").contains(section))
            throw new IllegalArgumentException("Invalid finance view");
        return service.financial(f, section, page, size);
    }

    @GetMapping("/attendance/{section}")
    public Data attendance(@PathVariable String section, @ModelAttribute Filter f, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) BigDecimal threshold) {
        if (!Set.of("summary", "trend", "batch-wise", "student-wise").contains(section))
            throw new IllegalArgumentException("Invalid attendance view");
        return service.attendance(f, section, page, size, threshold);
    }

    @GetMapping("/academics/{section}")
    public Data academics(@PathVariable String section, @ModelAttribute Filter f, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        if (!Set.of("summary", "exam-trend", "course-wise", "batch-wise", "assignment-performance").contains(section))
            throw new IllegalArgumentException("Invalid academic view");
        return service.academic(f, section, page, size);
    }

    @GetMapping("/batches")
    public Data batches(@ModelAttribute Filter f, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "name,asc") String sort) {
        return service.batches(f, page, size, sort);
    }

    @GetMapping("/batches/{id}")
    public Data batch(@PathVariable Long id, @ModelAttribute Filter f) {
        return service.batches(new Filter(f.startDate(), f.endDate(), f.courseId(), id, f.teacherId(), f.studentId(), null, f.examType()), 0, 1);
    }

    @GetMapping("/teachers")
    public Data teachers(@ModelAttribute Filter f, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.teachers(f, page, size);
    }

    @GetMapping("/teachers/{id}")
    public Data teacher(@PathVariable Long id, @ModelAttribute Filter f) {
        return service.teachers(new Filter(f.startDate(), f.endDate(), f.courseId(), f.batchId(), id, null, null, null), 0, 1);
    }

    @GetMapping("/students/{id}/progress")
    public Map<String, Object> progress(@PathVariable Long id, @ModelAttribute Filter f) {
        return service.progress(id, f);
    }
}
