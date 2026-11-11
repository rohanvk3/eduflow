package com.eduflow.backend.dto;
import jakarta.validation.constraints.*;
import java.time.*;
import com.eduflow.backend.entity.*;
public record WithdrawalRequest(@NotNull LocalDate withdrawalDate) {}

