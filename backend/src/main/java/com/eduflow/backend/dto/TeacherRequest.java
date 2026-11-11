package com.eduflow.backend.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/** PUT requires identity fields; null optional fields preserve their existing values. */
public record TeacherRequest(
 @NotBlank @Size(max=50) @Pattern(regexp="[A-Za-z0-9_-]+") String employeeId,
 @NotBlank @Size(max=100) String fullName,
 @NotBlank @Email @Size(max=254) String email,
 @Pattern(regexp="\\+?[0-9]{7,15}|", message="must contain 7–15 digits with optional leading +") String phone,
 @Size(max=30) String gender,
 @Past LocalDate dateOfBirth,
 @Size(max=200) String qualification,
 @Size(max=200) String specialization,
 @Size(max=30) Set<@NotBlank @Size(max=100) String> subjects,
 @Min(0) @Max(100) Integer experienceYears,
 @PastOrPresent LocalDate joiningDate,
 @Size(max=500) String address,
 @DecimalMin("0.00") @Digits(integer=10, fraction=2) BigDecimal salary,
 @Positive Long userId
) {}

