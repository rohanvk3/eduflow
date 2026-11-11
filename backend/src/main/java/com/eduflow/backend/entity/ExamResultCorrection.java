package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="exam_result_corrections")
public class ExamResultCorrection extends ExamRecordBase {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private ExamResult result;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal previousMarks;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal newMarks;
 @Column(nullable=false,length=1000) private String reason;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private User correctedBy;
 @Column(nullable=false) private Instant correctedAt;
 public ExamResult getResult(){return result;}
 public void setResult(ExamResult value){result=value;}
 public BigDecimal getPreviousMarks(){return previousMarks;}
 public void setPreviousMarks(BigDecimal value){previousMarks=value;}
 public BigDecimal getNewMarks(){return newMarks;}
 public void setNewMarks(BigDecimal value){newMarks=value;}
 public String getReason(){return reason;}
 public void setReason(String value){reason=value;}
 public User getCorrectedBy(){return correctedBy;}
 public void setCorrectedBy(User value){correctedBy=value;}
 public Instant getCorrectedAt(){return correctedAt;}
 public void setCorrectedAt(Instant value){correctedAt=value;}
}

