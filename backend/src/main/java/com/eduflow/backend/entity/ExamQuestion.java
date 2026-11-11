package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="exam_questions")
public class ExamQuestion extends ExamRecordBase {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Exam exam;
 @Column(nullable=false,length=6000) private String questionText;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal marks;
 @Column(nullable=false) private Integer displayOrder;
 public Exam getExam(){return exam;}
 public void setExam(Exam value){exam=value;}
 public String getQuestionText(){return questionText;}
 public void setQuestionText(String value){questionText=value;}
 public BigDecimal getMarks(){return marks;}
 public void setMarks(BigDecimal value){marks=value;}
 public Integer getDisplayOrder(){return displayOrder;}
 public void setDisplayOrder(Integer value){displayOrder=value;}
}

