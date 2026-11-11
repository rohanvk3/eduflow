package com.eduflow.backend.dto;
import jakarta.validation.constraints.*;
import java.time.*;
import com.eduflow.backend.entity.*;
public record CourseRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]+") @Size(max=40) String code, @NotBlank @Size(max=150) String name, @Size(max=2000) String description, @NotNull @Min(1) @Max(520) Integer durationWeeks) {}

