package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="exam_options")
public class ExamOption extends ExamRecordBase {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private ExamQuestion question;
 @Column(nullable=false,length=2000) private String optionText;
 @Column(name="is_correct",nullable=false) private boolean correct;
 @Column(nullable=false) private Integer displayOrder;
 public ExamQuestion getQuestion(){return question;}
 public void setQuestion(ExamQuestion value){question=value;}
 public String getOptionText(){return optionText;}
 public void setOptionText(String value){optionText=value;}
 public boolean getCorrect(){return correct;}
 public void setCorrect(boolean value){correct=value;}
 public Integer getDisplayOrder(){return displayOrder;}
 public void setDisplayOrder(Integer value){displayOrder=value;}
}

