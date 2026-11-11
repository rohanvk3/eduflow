package com.eduflow.backend.dto;
import jakarta.validation.constraints.*;
import java.time.*;
import com.eduflow.backend.entity.*;
public record BatchResponse(Long id, String code, String name, Long courseId, String courseName, Integer capacity, long activeEnrollments, LocalDate startDate, LocalDate endDate, BatchStatus status) {}

