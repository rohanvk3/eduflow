package com.eduflow.backend.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name="batch_schedules", indexes=@Index(name="ix_schedule_teacher_day", columnList="teacher_id,day_of_week,active"))
public class BatchSchedule {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="batch_id", nullable=false)
    private Batch batch;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="teacher_id", nullable=false)
    private Teacher teacher;
    @Enumerated(EnumType.STRING) @Column(name="day_of_week",nullable=false,length=12)
    private DayOfWeek dayOfWeek;
    @Column(nullable=false)
    private LocalTime startTime;
    @Column(nullable=false)
    private LocalTime endTime;
    @Column(length=100)
    private String room;
    @Column(nullable=false)
    private boolean active = true;
    @Column(nullable=false, updatable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime updatedAt;
    public BatchSchedule() {}
    @PrePersist void onCreate() { createdAt=LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")); updatedAt=createdAt; }
    @PreUpdate void onUpdate() { updatedAt=LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")); }
    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Batch getBatch() { return batch; }
    public void setBatch(Batch value) { batch=value; }
    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher value) { teacher=value; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek value) { dayOfWeek=value; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime value) { startTime=value; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime value) { endTime=value; }
    public String getRoom() { return room; }
    public void setRoom(String value) { room=value; }
    public boolean getActive() { return active; }
    public void setActive(boolean value) { active=value; }
}

