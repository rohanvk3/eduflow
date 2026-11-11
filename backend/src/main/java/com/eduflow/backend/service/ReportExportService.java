package com.eduflow.backend.service;

import com.eduflow.backend.dto.AnalyticsDtos.Data;
import com.eduflow.backend.dto.AnalyticsDtos.Filter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class ReportExportService {
    final AnalyticsService analytics;
    final InstituteSettingsService institute;

    public ReportExportService(AnalyticsService analytics, InstituteSettingsService institute) {
        this.analytics = analytics;
        this.institute = institute;
    }

    public Data preview(String type, Filter f, int page, int size) {
        return switch (type) {
            case "REVENUE" -> analytics.financial(f, "revenue-trend", page, size);
            case "OUTSTANDING_FEES" -> analytics.financial(f, "outstanding", page, size);
            case "PAYMENT_COLLECTIONS" -> analytics.financial(f, "payments", page, size);
            case "ATTENDANCE" -> analytics.attendance(f, "student-wise", page, size, null);
            case "STUDENT_PERFORMANCE" -> analytics.academic(f, "students", page, size);
            case "EXAM_RESULTS" -> analytics.academic(f, "exam-trend", page, size);
            case "ASSIGNMENT_COMPLETION" -> analytics.academic(f, "assignment-performance", page, size);
            case "BATCH_PERFORMANCE" -> analytics.batches(f, page, size);
            case "TEACHER_WORKLOAD" -> analytics.teachers(f, page, size);
            default -> throw new IllegalArgumentException("Invalid reportType");
        };
    }

    Data bounded(String type, Filter f, int limit) {
        var first = preview(type, f, 0, 100);
        var rows = new ArrayList<>(first.rows());
        int page = 0;
        var next = first;
        while (next.hasMore()) {
            if (rows.size() >= limit)
                throw new IllegalArgumentException("Report exceeds " + limit + " rows; narrow filters");
            next = preview(type, f, ++page, 100);
            rows.addAll(next.rows());
        }
        return new Data(first.title(), first.startDate(), first.endDate(), first.metrics(), rows, 0, rows.size(), false);
    }

    public byte[] csv(String type, Filter f) {
        var d = bounded(type, f, type.equals("BATCH_PERFORMANCE") ? 100 : 2000);
        var out = new StringBuilder("\uFEFF");
        out.append("Report,").append(FinanceReportService.csvCell(d.title())).append("\r\nPeriod,").append(d.startDate()).append(" to ").append(d.endDate()).append("\r\n");
        out.append("Filters,").append(FinanceReportService.csvCell(f.toString())).append("\r\n");
        d.metrics().forEach((k, v) -> out.append(FinanceReportService.csvCell(k)).append(',').append(FinanceReportService.csvCell(Objects.toString(v, ""))).append("\r\n"));
        if (!d.rows().isEmpty()) {
            var keys = d.rows().getFirst().keySet();
            out.append(String.join(",", keys.stream().map(FinanceReportService::csvCell).toList())).append("\r\n");
            for (var row : d.rows())
                out.append(String.join(",", keys.stream().map(k -> FinanceReportService.csvCell(Objects.toString(row.get(k), ""))).toList())).append("\r\n");
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] pdf(String type, Filter f) throws IOException {
        var d = bounded(type, f, type.equals("BATCH_PERFORMANCE") ? 100 : 500);
        try (var doc = new PDDocument(); var out = new ByteArrayOutputStream()) {
            var font = PDType0Font.load(doc, new org.springframework.core.io.ClassPathResource("fonts/NotoSans-Regular.ttf").getInputStream());
            var lines = new ArrayList<String>();
            lines.add(institute.instituteName() + " | " + d.title());
            lines.add("Period (inclusive IST): " + d.startDate() + " to " + d.endDate());
            lines.add("Generated: " + Instant.now());
            lines.add("Filters: course=" + f.courseId() + ", batch=" + f.batchId() + ", teacher=" + f.teacherId() + ", student=" + f.studentId() + ", status=" + f.paymentStatus() + ", exam=" + f.examType());
            lines.add(" ");
            d.metrics().forEach((k, v) -> lines.add(k + ": " + Objects.toString(v, "No data")));
            lines.add(" ");
            if (d.rows().isEmpty()) lines.add("No records in selected scope.");
            for (var row : d.rows()) {
                lines.add("Field                          | Value");
                row.forEach((k, v) -> lines.add(k + " | " + Objects.toString(v, "No data")));
                lines.add("------------------------------------------------------------");
            }
            var wrapped = new ArrayList<String>();
            for (String line : lines) {
                StringBuilder cleaned = new StringBuilder();
                for (int c : line.codePoints().toArray()) {
                    String ch = new String(Character.toChars(c));
                    try {
                        font.getStringWidth(ch);
                        cleaned.append(ch);
                    } catch (IllegalArgumentException ex) {
                        cleaned.append("?");
                    }
                }
                String safe = cleaned.toString();
                StringBuilder lineBuffer=new StringBuilder();
                for(int cp:safe.codePoints().toArray()){
                    String ch=new String(Character.toChars(cp));
                    if(font.getStringWidth(lineBuffer+ch)/1000*10>510 && !lineBuffer.isEmpty()){
                        wrapped.add(lineBuffer.toString());lineBuffer.setLength(0);
                    }
                    lineBuffer.append(ch);
                }
                wrapped.add(lineBuffer.toString());
            }
            int pages = (wrapped.size() + 45) / 46;
            for (int i = 0; i < pages; i++) {
                var page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                try (var content = new PDPageContentStream(doc, page)) {
                    content.beginText();
                    content.setFont(font, 10);
                    content.setLeading(16);
                    content.newLineAtOffset(40, 790);
                    for (int n = i * 46; n < Math.min((i + 1) * 46, wrapped.size()); n++) {
                        content.showText(wrapped.get(n));
                        content.newLine();
                    }
                    content.endText();
                    content.beginText();
                    content.setFont(font, 9);
                    content.newLineAtOffset(40, 30);
                    content.showText("EduFlow report | Page " + (i + 1) + " of " + pages);
                    content.endText();
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
