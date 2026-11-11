package com.eduflow.backend.dto;
import com.eduflow.backend.entity.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
public final class ParentDtos {
 private ParentDtos(){}
 public record CreateProfile(Long userId,@Size(max=100)String fullName,@Email @Size(max=254)String email,@Size(min=8,max=72)String password,@NotBlank @Pattern(regexp="\\+?[0-9]{7,15}")String phoneNumber,@Pattern(regexp="(?:\\+?[0-9]{7,15})?")String alternatePhone,@Size(max=500)String address){}
 public record EditProfile(@NotNull Long version,@NotBlank @Size(max=100)String fullName,@NotBlank @Pattern(regexp="\\+?[0-9]{7,15}")String phoneNumber,@Pattern(regexp="(?:\\+?[0-9]{7,15})?")String alternatePhone,@Email @Size(max=254)String email,@Size(max=500)String address){}
 public record AdminProfile(@NotNull Long version,@NotBlank @Size(max=100)String fullName,@NotBlank @Pattern(regexp="\\+?[0-9]{7,15}")String phoneNumber,@Pattern(regexp="(?:\\+?[0-9]{7,15})?")String alternatePhone,@Email @Size(max=254)String email,@Size(max=500)String address,@NotNull StudentStatus status){}
 public record ProfileView(Long id,Long userId,String fullName,String loginEmail,String email,String phoneNumber,String alternatePhone,String address,StudentStatus status,long version,LocalDateTime createdAt){}
 public record UserChoice(Long id,String fullName,String email){}
 public record LinkRequest(@NotNull Long parentId,@NotNull Long studentId,@NotNull RelationshipType relationshipType,boolean primaryGuardian,@NotBlank @Size(max=1000)String reason){}
 public record LinkAction(@NotNull Long version,@NotBlank @Size(max=1000)String reason){}
 public record LinkView(Long id,Long parentId,String parentName,Long studentId,String studentName,String admissionNumber,RelationshipType relationshipType,ParentLinkStatus status,boolean primaryGuardian,String verifiedBy,Instant verifiedAt,long version){}
 public record AuditView(Long id,ParentLinkStatus previousStatus,ParentLinkStatus newStatus,RelationshipType relationshipType,boolean primaryGuardian,String changedBy,Instant changedAt,String reason){}
 public record EnrollmentView(Long id,Long courseId,String courseName,Long batchId,String batchName,BatchStatus batchStatus,EnrollmentStatus status,LocalDate enrollmentDate,LocalDate withdrawalDate,LocalDate completionDate,boolean active){}
 public record ChildView(Long id,String fullName,String admissionNumber,StudentStatus status,RelationshipType relationshipType,boolean primaryGuardian,List<EnrollmentView> enrollments){}
 public record FeeTotals(BigDecimal totalPayable,BigDecimal totalPaid,BigDecimal outstanding,BigDecimal overdue){}
 public record PaymentView(Long id,String receiptNumber,BigDecimal amount,LocalDate paymentDate,PaymentMethod paymentMethod,FeePaymentStatus status){}
 public record FeeAccountView(Long id,String courseName,String batchName,BigDecimal totalPayable,BigDecimal paidAmount,BigDecimal outstandingAmount,BigDecimal overdueAmount,List<FinanceDtos.InstallmentResponse> installments,List<PaymentView> payments){}
 public record Fees(FeeTotals totals,PageResponse<FeeAccountView> accounts){}
 public record UpcomingExam(Long id,String title,String courseName,String batchName,Instant startDateTime,Instant endDateTime){}
 public record ResultView(Long id,Long examId,String examTitle,Long courseId,String courseName,Long batchId,String batchName,Instant examDate,BigDecimal obtainedMarks,BigDecimal totalMarks,BigDecimal percentage,String grade,boolean passed){}
 public record AssignmentView(Long id,String title,Long courseId,String courseName,Long batchId,String batchName,Instant dueDateTime,AssignmentStatus assignmentStatus,String submissionStatus,Instant submittedAt,boolean late,BigDecimal maxMarks,BigDecimal obtainedMarks,String feedback,Instant gradedAt){}
 public record ResourceView(Long id,String title,String description,String courseName,String batchName,String categoryName,ResourceType resourceType,Instant publishedAt){}
 public record Progress(LocalDate fromDate,LocalDate toDate,AttendanceDtos.Totals attendance,List<AttendanceDtos.Trend> attendanceTrend,long eligibleAssignments,long submittedAssignments,BigDecimal assignmentCompletionPercentage,BigDecimal averageExamPercentage,List<ResultView> examTrend,List<AssignmentView> assignmentTrend,List<CourseProgress> courses){}
 public record CourseProgress(Long id,String name,long publishedResults,BigDecimal averageExamPercentage,long eligibleAssignments,long submittedAssignments,BigDecimal assignmentCompletionPercentage){}
 public record Activity(String type,String title,Instant occurredAt){}
 public record Dashboard(boolean linked,ChildView child,AttendanceDtos.Totals attendance,FeeTotals fees,long upcomingExamsCount,long pendingAssignments,List<UpcomingExam> upcomingExams,List<ResultView> recentResults,List<AssignmentView> upcomingAssignments,List<FeeAccountView> feeAccounts,List<Activity> activity,Progress progress){}
}

