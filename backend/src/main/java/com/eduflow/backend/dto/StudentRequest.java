package com.eduflow.backend.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record StudentRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "\\+?[0-9]{7,15}", message = "must contain 7–15 digits with an optional leading +") String phone,
        @NotNull @Past LocalDate dateOfBirth,
        @Size(max = 30) String gender,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Size(max = 100) String guardianName,
        @NotBlank @Pattern(regexp = "\\+?[0-9]{7,15}", message = "must contain 7–15 digits with an optional leading +") String guardianPhone,
        @NotNull @PastOrPresent LocalDate admissionDate,
        @Positive Long userId) {
}
