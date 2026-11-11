package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="submission_grade_history")
public class SubmissionGradeHistory extends AssignmentRecordBase{
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private AssignmentSubmission submission;
 @Column(nullable=true,precision=10,scale=2) private BigDecimal previousMarks;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal newMarks;
 @Column(nullable=true,length=5000) private String previousFeedback;
 @Column(nullable=true,length=5000) private String newFeedback;
 @Column(nullable=false,length=1000) private String reason;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private User gradedBy;
 @Column(nullable=false) private Instant gradedAt;
 public AssignmentSubmission getSubmission(){return submission;}
 public void setSubmission(AssignmentSubmission value){submission=value;}
 public BigDecimal getPreviousMarks(){return previousMarks;}
 public void setPreviousMarks(BigDecimal value){previousMarks=value;}
 public BigDecimal getNewMarks(){return newMarks;}
 public void setNewMarks(BigDecimal value){newMarks=value;}
 public String getPreviousFeedback(){return previousFeedback;}
 public void setPreviousFeedback(String value){previousFeedback=value;}
 public String getNewFeedback(){return newFeedback;}
 public void setNewFeedback(String value){newFeedback=value;}
 public String getReason(){return reason;}
 public void setReason(String value){reason=value;}
 public User getGradedBy(){return gradedBy;}
 public void setGradedBy(User value){gradedBy=value;}
 public Instant getGradedAt(){return gradedAt;}
 public void setGradedAt(Instant value){gradedAt=value;}
}

