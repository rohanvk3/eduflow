package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="exam_answers",uniqueConstraints=@UniqueConstraint(name="uk_exam_answer_question",columnNames={"attempt_id","question_id"}))
public class ExamAnswer extends ExamRecordBase {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private ExamAttempt attempt;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private ExamQuestion question;
 @ManyToOne(fetch=FetchType.LAZY,optional=true) @JoinColumn(nullable=true) private ExamOption selectedOption;
 @Column(precision=10,scale=2) private BigDecimal awardedMarks;
 public ExamAttempt getAttempt(){return attempt;}
 public void setAttempt(ExamAttempt value){attempt=value;}
 public ExamQuestion getQuestion(){return question;}
 public void setQuestion(ExamQuestion value){question=value;}
 public ExamOption getSelectedOption(){return selectedOption;}
 public void setSelectedOption(ExamOption value){selectedOption=value;}
 public BigDecimal getAwardedMarks(){return awardedMarks;}
 public void setAwardedMarks(BigDecimal value){awardedMarks=value;}
}

