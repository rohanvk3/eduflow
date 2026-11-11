package com.eduflow.backend.dto;
import jakarta.validation.constraints.*;
import java.time.*;
import com.eduflow.backend.entity.*;
public record CourseResponse(Long id, String code, String name, String description, Integer durationWeeks, StudentStatus status) {}

