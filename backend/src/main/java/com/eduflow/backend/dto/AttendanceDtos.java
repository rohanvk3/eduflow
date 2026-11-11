package com.eduflow.backend.dto;

import com.eduflow.backend.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.List;

/** API-only data: entities, credentials and salaries never leave the service. */
public final class AttendanceDtos {
 private AttendanceDtos() {}
 public record CreateSession(@NotNull @Positive Long batchId,@NotNull LocalDate sessionDate,@Positive Long scheduleId,LocalTime startTime,LocalTime endTime,@Size(max=250) String topic) {}
 public record RecordInput(@NotNull @Positive Long studentId,@NotNull AttendanceStatus status,@Size(max=500) String remarks) {}
 public record SaveRecords(@NotNull @Min(0) Long version,@NotNull @Size(max=10000) List<@NotNull @Valid RecordInput> records) {}
 public record FinalizeRequest(@NotNull @Min(0) Long version) {}
 public record SessionSummary(Long id,Long batchId,String batchName,Long courseId,String courseName,LocalDate sessionDate,Long scheduleId,LocalTime startTime,LocalTime endTime,String topic,AttendanceSessionStatus status,String markedBy,LocalDateTime finalizedAt,Long version,long eligibleCount,long markedCount) {}
 public record RegisterRow(Long studentId,String fullName,String admissionNumber,AttendanceStatus status,String remarks) {}
 public record SessionDetails(SessionSummary session,List<RegisterRow> students,long staleRecords) {}
 public record Totals(long totalEligibleFinalizedSessions,long presentCount,long lateCount,long absentCount,long excusedCount,BigDecimal attendancePercentage,long finalizedSessions) {}
 public record GroupSummary(Long id,String name,String code,Totals totals) {}
 public record Trend(LocalDate date,Totals totals) {}
 public record History(Long sessionId,Long batchId,String batchName,String courseName,LocalDate sessionDate,LocalTime startTime,LocalTime endTime,String topic,AttendanceStatus status,String remarks) {}
 public record Report(Totals totals,PageResponse<GroupSummary> students,PageResponse<GroupSummary> batches,List<Trend> trend,LocalDate fromDate,LocalDate toDate) {}
 public record Personal(boolean linked,Totals totals,PageResponse<GroupSummary> batches,List<Trend> trend,PageResponse<History> history,LocalDate fromDate,LocalDate toDate) {}
 public record Overview(Totals totals,long pendingDrafts,PageResponse<GroupSummary> batches,PageResponse<SessionSummary> recentFinalized,LocalDate fromDate,LocalDate toDate) {}
}

