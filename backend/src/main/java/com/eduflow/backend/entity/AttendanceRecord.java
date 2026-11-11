package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name="attendance_records", uniqueConstraints=@UniqueConstraint(name="uk_attendance_session_student",columnNames={"session_id","student_id"}), indexes=@Index(name="ix_attendance_student_session",columnList="student_id,session_id"))
public class AttendanceRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="session_id",nullable=false) private AttendanceSession session;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="student_id",nullable=false) private Student student;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private AttendanceStatus status;
    @Column(length=500) private String remarks;
    @Column(nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime updatedAt;
    public AttendanceRecord() {}
    @PrePersist void onCreate() { createdAt=LocalDateTime.now(ZoneId.of("Asia/Kolkata")); updatedAt=createdAt; }
    @PreUpdate void onUpdate() { updatedAt=LocalDateTime.now(ZoneId.of("Asia/Kolkata")); }
    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public AttendanceSession getSession() { return session; }
    public void setSession(AttendanceSession value) { session=value; }
    public Student getStudent() { return student; }
    public void setStudent(Student value) { student=value; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus value) { status=value; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String value) { remarks=value; }
}

