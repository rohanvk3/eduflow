package com.eduflow.backend.dto;
import jakarta.validation.constraints.*;
import java.time.*;
import com.eduflow.backend.entity.*;
public record ScheduleResponse(Long id, Long batchId, Long teacherId, String teacherName, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, String room, boolean active) {}

