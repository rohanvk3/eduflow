package com.eduflow.backend.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class AnalyticsDtos {
    public record Filter(LocalDate startDate, LocalDate endDate, Long courseId, Long batchId, Long teacherId,
                         Long studentId, String paymentStatus, String examType) {
    }

    public record Data(String title, LocalDate startDate, LocalDate endDate, Map<String, Object> metrics,
                       List<Map<String, Object>> rows, int page, int size, boolean hasMore) {
    }
}
