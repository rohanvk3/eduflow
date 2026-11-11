package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="assignment_submissions",uniqueConstraints=@UniqueConstraint(name="uk_assignment_submission",columnNames={"assignment_id","student_id"}),indexes=@Index(name="ix_submission_status",columnList="status,submitted_at"))
public class AssignmentSubmission extends AssignmentRecordBase{
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Assignment assignment;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Student student;
 @Column(columnDefinition="MEDIUMTEXT") private String submissionText;
 @Column(nullable=false) private Instant submittedAt;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private SubmissionStatus status=SubmissionStatus.SUBMITTED;
 @Column(nullable=false) private boolean late;
 @Column(nullable=true,precision=10,scale=2) private BigDecimal obtainedMarks;
 @Column(nullable=true,length=5000) private String feedback;
 @ManyToOne(fetch=FetchType.LAZY,optional=true) @JoinColumn(nullable=true) private User gradedBy;
  private Instant gradedAt;
 @Version private long version;
 public Assignment getAssignment(){return assignment;}
 public void setAssignment(Assignment value){assignment=value;}
 public Student getStudent(){return student;}
 public void setStudent(Student value){student=value;}
 public String getSubmissionText(){return submissionText;}
 public void setSubmissionText(String value){submissionText=value;}
 public Instant getSubmittedAt(){return submittedAt;}
 public void setSubmittedAt(Instant value){submittedAt=value;}
 public SubmissionStatus getStatus(){return status;}
 public void setStatus(SubmissionStatus value){status=value;}
 public boolean getLate(){return late;}
 public void setLate(boolean value){late=value;}
 public BigDecimal getObtainedMarks(){return obtainedMarks;}
 public void setObtainedMarks(BigDecimal value){obtainedMarks=value;}
 public String getFeedback(){return feedback;}
 public void setFeedback(String value){feedback=value;}
 public User getGradedBy(){return gradedBy;}
 public void setGradedBy(User value){gradedBy=value;}
 public Instant getGradedAt(){return gradedAt;}
 public void setGradedAt(Instant value){gradedAt=value;}
 public long getVersion(){return version;}
}

