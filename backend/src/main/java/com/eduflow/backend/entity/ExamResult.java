package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="exam_results",uniqueConstraints=@UniqueConstraint(name="uk_exam_result_student",columnNames={"exam_id","student_id"}))
public class ExamResult extends ExamRecordBase {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Exam exam;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Student student;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal obtainedMarks;
 @Column(length=1000) private String remarks;
 @ManyToOne(fetch=FetchType.LAZY,optional=true) @JoinColumn(nullable=true) private User evaluatedBy;
 @Column(nullable=false) private Instant evaluatedAt;
 public Exam getExam(){return exam;}
 public void setExam(Exam value){exam=value;}
 public Student getStudent(){return student;}
 public void setStudent(Student value){student=value;}
 public BigDecimal getObtainedMarks(){return obtainedMarks;}
 public void setObtainedMarks(BigDecimal value){obtainedMarks=value;}
 public String getRemarks(){return remarks;}
 public void setRemarks(String value){remarks=value;}
 public User getEvaluatedBy(){return evaluatedBy;}
 public void setEvaluatedBy(User value){evaluatedBy=value;}
 public Instant getEvaluatedAt(){return evaluatedAt;}
 public void setEvaluatedAt(Instant value){evaluatedAt=value;}
}

