package com.eduflow.backend.dto;
import com.eduflow.backend.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
public final class AssignmentDtos {
 private AssignmentDtos(){}
 public record AssignmentRequest(@NotBlank @Size(max=200) String title,@Size(max=10000) String description,
  @NotNull Long batchId,@NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=8,fraction=2) BigDecimal maxMarks,
  @NotNull Instant dueDateTime,boolean allowLateSubmission,Long version){}
 public record VersionRequest(@NotNull Long version){}
 public record FileView(Long id,String originalFileName,String contentType,long fileSize,LocalDateTime createdAt){}
 public record Counts(long eligible,long submitted,long pending,long late,long graded,BigDecimal completionPercentage){}
 public record AssignmentView(Long id,String title,String description,Long batchId,String batchName,String courseName,String createdBy,
  BigDecimal maxMarks,Instant publishedAt,Instant dueDateTime,boolean allowLateSubmission,AssignmentStatus status,long version,
  Instant serverNow,Counts counts,Long ownSubmissionId,SubmissionStatus ownSubmissionStatus){}
 public record SubmissionRequest(@Size(max=20000) String submissionText,Long version,@Size(max=10) List<@NotNull Long> keepAttachmentIds){}
 public record SubmissionView(Long id,Long assignmentId,String assignmentTitle,String batchName,String courseName,Long studentId,
  String studentName,String admissionNumber,String submissionText,Instant submittedAt,SubmissionStatus status,boolean late,
  BigDecimal maxMarks,BigDecimal obtainedMarks,String feedback,String gradedBy,Instant gradedAt,long version,List<FileView> attachments){}
 public record GradeRequest(@NotNull Long version,@NotNull @DecimalMin("0") @Digits(integer=8,fraction=2) BigDecimal obtainedMarks,
  @Size(max=5000) String feedback,@Size(max=1000) String reason){}
 public record RevisionView(Long id,long revisionNumber,String submissionText,Instant submittedAt,SubmissionStatus status,
  String changedBy,Instant changedAt,List<FileView> attachments){}
 public record GradeHistoryView(Long id,BigDecimal previousMarks,BigDecimal newMarks,String previousFeedback,String newFeedback,
  String reason,String gradedBy,Instant gradedAt){}
 public record History(List<RevisionView> revisions,List<GradeHistoryView> grades){}
 public record RecipientView(Long studentId,String studentName,String admissionNumber){}
 public record BatchCompletion(Long batchId,String batchName,long eligible,long submitted,BigDecimal completionPercentage){}
 public record BatchActivity(LocalDate day,Long batchId,String batchName,long submissions){}
 public record Statistics(long totalAssignments,long publishedAssignments,long createdByMe,long eligibleSubmissions,
  long pendingSubmissions,long submittedAssignments,long lateSubmissions,long gradedSubmissions,long overdueAssignments,
  BigDecimal completionPercentage,BigDecimal gradingPercentage,BigDecimal averageMarks,BigDecimal averagePercentage,
  List<BatchCompletion> batches,List<AssignmentView> upcoming,List<SubmissionView> recentSubmissions,List<BatchActivity> activity,Instant serverNow){}
}
