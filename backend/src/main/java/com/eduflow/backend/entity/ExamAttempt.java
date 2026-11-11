package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="exam_attempts",uniqueConstraints=@UniqueConstraint(name="uk_exam_attempt_student",columnNames={"exam_id","student_id"}),indexes=@Index(name="ix_attempt_expiry",columnList="status,expires_at"))
public class ExamAttempt extends ExamRecordBase {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Exam exam;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Student student;
 @Column(nullable=false) private Instant startedAt;
 @Column(nullable=false) private Instant expiresAt;
  private Instant submittedAt;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ExamAttemptStatus status=ExamAttemptStatus.IN_PROGRESS;
 @Column(precision=10,scale=2) private BigDecimal obtainedMarks;
 @Version private long version;
 public Exam getExam(){return exam;}
 public void setExam(Exam value){exam=value;}
 public Student getStudent(){return student;}
 public void setStudent(Student value){student=value;}
 public Instant getStartedAt(){return startedAt;}
 public void setStartedAt(Instant value){startedAt=value;}
 public Instant getExpiresAt(){return expiresAt;}
 public void setExpiresAt(Instant value){expiresAt=value;}
 public Instant getSubmittedAt(){return submittedAt;}
 public void setSubmittedAt(Instant value){submittedAt=value;}
 public ExamAttemptStatus getStatus(){return status;}
 public void setStatus(ExamAttemptStatus value){status=value;}
 public BigDecimal getObtainedMarks(){return obtainedMarks;}
 public void setObtainedMarks(BigDecimal value){obtainedMarks=value;}
 public long getVersion(){return version;}
}

