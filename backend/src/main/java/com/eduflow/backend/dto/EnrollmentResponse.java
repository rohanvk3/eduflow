package com.eduflow.backend.dto;
import jakarta.validation.constraints.*;
import java.time.*;
import com.eduflow.backend.entity.*;
public record EnrollmentResponse(Long id, Long batchId, String batchName, Long studentId, String fullName, String admissionNumber, LocalDate enrollmentDate, LocalDate withdrawalDate, LocalDate completionDate, EnrollmentStatus status) {}

