package com.eduflow.backend.dto;
import com.eduflow.backend.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public final class ExamDtos {
 private ExamDtos(){}
 public record ExamRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,40}") String examCode,
  @NotBlank @Size(max=200) String title,@Size(max=4000) String description,@NotNull Long batchId,@NotNull ExamType examType,
  @Min(1) @Max(1440) Integer durationMinutes,@NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=8,fraction=2) BigDecimal totalMarks,
  @NotNull @DecimalMin("0") @Digits(integer=8,fraction=2) BigDecimal passingMarks,@NotNull Instant startDateTime,@NotNull Instant endDateTime,Long version){}
 public record ExamView(Long id,String examCode,String title,String description,Long batchId,String batchName,String courseName,
  ExamType examType,Integer durationMinutes,BigDecimal totalMarks,BigDecimal passingMarks,Instant startDateTime,Instant endDateTime,
  ExamStatus status,boolean resultsPublished,Instant resultsPublishedAt,long version){}
 public record OptionInput(@NotBlank @Size(max=2000) String optionText,boolean correct){}
 public record QuestionRequest(@NotBlank @Size(max=6000) String questionText,
  @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=8,fraction=2) BigDecimal marks,
  @NotNull @Positive Integer displayOrder,@Size(min=2,max=10) @NotNull List<@Valid OptionInput> options,@NotNull Long version){}
 public record StaffOption(Long id,String optionText,boolean correct){}
 public record StaffQuestion(Long id,String questionText,BigDecimal marks,Integer displayOrder,List<StaffOption> options){}
 public record StudentOption(Long id,String optionText){}
 public record StudentQuestion(Long id,String questionText,BigDecimal marks,Integer displayOrder,List<StudentOption> options){}
 public record VersionRequest(@NotNull Long version){}
 public record CancelRequest(@NotNull Long version,@NotBlank @Size(max=1000) String reason){}
 public record OrderRequest(@NotNull Long version,@NotEmpty @Size(max=200) List<@NotNull Long> questionIds){}
 public record AnswerInput(@NotNull Long questionId,Long selectedOptionId){}
 public record SaveAnswers(@NotNull Long version,@NotNull @Size(max=200) List<@Valid AnswerInput> answers){}
 public record SavedAnswer(Long questionId,Long selectedOptionId){}
 public record AttemptView(Long id,Long examId,String title,ExamAttemptStatus status,Instant startedAt,Instant expiresAt,Instant serverNow,
  long version,boolean answersAccepted,List<StudentQuestion> questions,List<SavedAnswer> answers){}
 public record OfflineInput(@NotNull Long studentId,@NotNull @DecimalMin("0") @Digits(integer=8,fraction=2) BigDecimal obtainedMarks,@Size(max=1000) String remarks){}
 public record OfflineRequest(@NotNull Long version,@NotEmpty @Size(max=500) List<@Valid OfflineInput> results){}
 public record GradebookRow(Long studentId,String admissionNumber,String studentName,BigDecimal obtainedMarks,String remarks){}
 public record Gradebook(ExamView exam,List<GradebookRow> students){}
 public record ResultView(Long id,Long examId,String examTitle,String batchName,String courseName,Instant examDate,
  Long studentId,String studentName,String admissionNumber,BigDecimal obtainedMarks,BigDecimal totalMarks,BigDecimal percentage,
  boolean passed,String grade,Instant evaluatedAt,String remarks){}
 public record ReviewQuestion(Long questionId,String questionText,BigDecimal marks,List<StudentOption> options,Long selectedOptionId,Long correctOptionId,BigDecimal awardedMarks){}
 public record OwnResult(boolean published,ResultView result,List<ReviewQuestion> review){}
 public record ResultSummary(long count,BigDecimal average,BigDecimal highest,BigDecimal lowest,BigDecimal passRate){}
 public record Results(ExamView exam,ResultSummary summary,PageResponse<ResultView> results){}
 public record RankedResult(long rank,ResultView result){}
 public record CorrectionRequest(@NotNull Long version,@NotNull @DecimalMin("0") @Digits(integer=8,fraction=2) BigDecimal obtainedMarks,@NotBlank @Size(max=1000) String reason){}
 public record CorrectionView(Long id,BigDecimal previousMarks,BigDecimal newMarks,String reason,String correctedBy,Instant correctedAt){}
 public record Metric(String label,long count,BigDecimal average,BigDecimal passRate){}
 public record Analytics(long totalExams,long publishedExams,long completedExams,long upcomingExams,long awaitingResults,
  ResultSummary scores,List<Metric> batches,List<Metric> courses,List<Metric> distribution,List<ResultView> trend,List<ExamView> upcoming,List<ExamView> needsGrading){}
}
