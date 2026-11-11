package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name="attendance_sessions", uniqueConstraints=@UniqueConstraint(name="uk_attendance_occurrence",columnNames={"batch_id","session_date","occurrence_key"}), indexes={@Index(name="ix_attendance_session_date",columnList="session_date"),@Index(name="ix_attendance_batch_date",columnList="batch_id,session_date")})
public class AttendanceSession {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="batch_id",nullable=false) private Batch batch;
    @Column(name="session_date",nullable=false) private LocalDate sessionDate;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="schedule_id") private BatchSchedule schedule;
     private LocalTime startTime;
     private LocalTime endTime;
    @Column(name="occurrence_key",nullable=false,length=20,updatable=false) private String occurrenceKey;
    @Column(length=250) private String topic;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private AttendanceSessionStatus status = AttendanceSessionStatus.DRAFT;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="marked_by_id",nullable=false) private User markedBy;
     private LocalDateTime finalizedAt;
    @Column(nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime updatedAt;
    public AttendanceSession() {}
    @PrePersist void onCreate() { createdAt=LocalDateTime.now(ZoneId.of("Asia/Kolkata")); updatedAt=createdAt; }
    @PreUpdate void onUpdate() { updatedAt=LocalDateTime.now(ZoneId.of("Asia/Kolkata")); }
    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    @Version private Long version;
    public Long getVersion() { return version; }
    public void touch() { updatedAt=LocalDateTime.now(ZoneId.of("Asia/Kolkata")); }
    public Batch getBatch() { return batch; }
    public void setBatch(Batch value) { batch=value; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate value) { sessionDate=value; }
    public BatchSchedule getSchedule() { return schedule; }
    public void setSchedule(BatchSchedule value) { schedule=value; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime value) { startTime=value; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime value) { endTime=value; }
    public String getOccurrenceKey() { return occurrenceKey; }
    public void setOccurrenceKey(String value) { occurrenceKey=value; }
    public String getTopic() { return topic; }
    public void setTopic(String value) { topic=value; }
    public AttendanceSessionStatus getStatus() { return status; }
    public void setStatus(AttendanceSessionStatus value) { status=value; }
    public User getMarkedBy() { return markedBy; }
    public void setMarkedBy(User value) { markedBy=value; }
    public LocalDateTime getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(LocalDateTime value) { finalizedAt=value; }
}

