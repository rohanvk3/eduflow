package com.eduflow.backend.dto;
import jakarta.validation.constraints.*;
import java.time.*;
import com.eduflow.backend.entity.*;
public record BatchRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]+") @Size(max=40) String code, @NotBlank @Size(max=150) String name, @NotNull @Positive Long courseId, @NotNull @Min(1) @Max(10000) Integer capacity, @NotNull LocalDate startDate, @NotNull LocalDate endDate, @NotNull BatchStatus status) {}

