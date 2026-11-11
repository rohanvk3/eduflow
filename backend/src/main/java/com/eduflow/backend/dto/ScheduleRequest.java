package com.eduflow.backend.dto;
import jakarta.validation.constraints.*;
import java.time.*;
import com.eduflow.backend.entity.*;
public record ScheduleRequest(@NotNull @Positive Long teacherId, @NotNull DayOfWeek dayOfWeek, @NotNull LocalTime startTime, @NotNull LocalTime endTime, @Size(max=100) String room, @NotNull Boolean active) {}

