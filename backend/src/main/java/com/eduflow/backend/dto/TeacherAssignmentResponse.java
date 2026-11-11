package com.eduflow.backend.dto;
import jakarta.validation.constraints.*;
import java.time.*;
import com.eduflow.backend.entity.*;
public record TeacherAssignmentResponse(Long id, Long teacherId, String fullName, String employeeId, boolean active) {}

