package com.eduflow.backend.controller;

import com.eduflow.backend.dto.AnalyticsDtos.Data;
import com.eduflow.backend.dto.AnalyticsDtos.Filter;
import com.eduflow.backend.service.ReportExportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {
    final ReportExportService service;

    public ReportsController(ReportExportService service) {
        this.service = service;
    }

    @GetMapping
    public Data preview(@RequestParam String reportType, @ModelAttribute Filter f, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.preview(reportType, f, page, size);
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> csv(@RequestParam String reportType, @ModelAttribute Filter f) {
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=eduflow-report.csv").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(service.csv(reportType, f));
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam String reportType, @ModelAttribute Filter f) throws IOException {
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=eduflow-report.pdf").contentType(MediaType.APPLICATION_PDF).body(service.pdf(reportType, f));
    }
}
