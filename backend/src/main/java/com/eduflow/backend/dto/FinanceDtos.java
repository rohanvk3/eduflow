package com.eduflow.backend.dto;

import com.eduflow.backend.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class FinanceDtos {
    private FinanceDtos() {}
    public record StructureRequest(@NotNull Long courseId, @NotBlank @Size(max=150) String name,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal tuitionFee,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal admissionFee,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal materialFee,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal otherFee,
        @NotNull LocalDate effectiveFrom, LocalDate effectiveTo, @NotNull StudentStatus status) {}
    public record StructureResponse(Long id, Long courseId, String courseName, String name, BigDecimal tuitionFee,
        BigDecimal admissionFee, BigDecimal materialFee, BigDecimal otherFee, BigDecimal totalFee,
        LocalDate effectiveFrom, LocalDate effectiveTo, StudentStatus status) {}
    public record AccountRequest(@NotNull Long enrollmentId, @NotNull Long feeStructureId,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal discountAmount, @NotNull LocalDate agreementDate) {}
    public record AccountResponse(Long id, Long studentId, String studentName, String admissionNumber,
        Long enrollmentId, EnrollmentStatus enrollmentStatus, Long courseId, String courseName, Long batchId, String batchName,
        Long feeStructureId, String structureName, LocalDate agreementDate, String currency,
        BigDecimal tuitionFee, BigDecimal admissionFee, BigDecimal materialFee, BigDecimal otherFee,
        BigDecimal originalAmount, BigDecimal discountAmount, BigDecimal finalPayableAmount,
        BigDecimal paidAmount, BigDecimal outstandingAmount, BigDecimal overdueAmount, String paymentStatus,
        FeeAccountStatus status, long version) {}
    public record AccountDetails(AccountResponse account, List<InstallmentResponse> installments, List<PaymentResponse> payments) {}
    public record PersonalFees(boolean linked, PageResponse<AccountResponse> accounts) {}
    public record InstallmentInput(@NotNull @Positive Integer installmentNumber,
        @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=2) BigDecimal amount,
        @NotNull LocalDate dueDate) {}
    public record ScheduleRequest(@NotNull Long version, @NotEmpty @Size(max=120) List<@Valid InstallmentInput> installments) {}
    public record InstallmentUpdate(@NotNull Long version, @NotNull LocalDate dueDate,
        @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=2) BigDecimal amount) {}
    public record InstallmentResponse(Long id, Integer installmentNumber, BigDecimal amount, LocalDate dueDate,
        BigDecimal paidAmount, BigDecimal outstandingAmount, String status) {}
    public record AllocationInput(@NotNull Long installmentId,
        @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=2) BigDecimal amount) {}
    public record PaymentRequest(@NotNull Long feeAccountId,
        @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=2) BigDecimal amount,
        @NotNull LocalDate paymentDate, @NotNull PaymentMethod paymentMethod,
        @Size(max=150) String transactionReference, @NotBlank @Size(max=100) String idempotencyKey,
        @NotEmpty @Size(max=120) List<@Valid AllocationInput> allocations, @Size(max=1000) String remarks) {}
    public record VoidRequest(@NotBlank @Size(max=1000) String reason) {}
    public record AllocationResponse(Long installmentId, Integer installmentNumber, BigDecimal amount) {}
    public record VoidResponse(String reason, String changedBy, LocalDateTime changedAt, String previousStatus,
        String newStatus, BigDecimal previousAmount) {}
    public record PaymentResponse(Long id, Long feeAccountId, String receiptNumber, Long studentId, String studentName,
        String admissionNumber, String courseName, String batchName, BigDecimal amount, LocalDate paymentDate,
        PaymentMethod paymentMethod, String transactionReference, FeePaymentStatus status, String recordedBy,
        String remarks, List<AllocationResponse> allocations, VoidResponse voidAudit, LocalDateTime createdAt) {}
    public record PaymentResult(PaymentResponse payment, boolean replayed) {}
    public record Metric(String label, BigDecimal amount) {}
    public record FinanceReport(String currency, String timezone, LocalDate fromDate, LocalDate toDate,
        BigDecimal totalAgreedFees, BigDecimal totalCollections, BigDecimal lifetimeCollections,
        BigDecimal outstandingBalance, BigDecimal overdueAmount, BigDecimal todayCollections, BigDecimal monthCollections,
        List<Metric> paymentMethods, List<Metric> monthlyTrend, List<Metric> courseCollections,
        List<Metric> batchOutstanding, PageResponse<PaymentResponse> payments) {}
}
