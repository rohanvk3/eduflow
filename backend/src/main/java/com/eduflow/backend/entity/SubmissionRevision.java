package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="submission_revisions",uniqueConstraints=@UniqueConstraint(name="uk_submission_revision",columnNames={"submission_id","revision_number"}))
public class SubmissionRevision extends AssignmentRecordBase{
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private AssignmentSubmission submission;
 @Column(nullable=false) private long revisionNumber;
 @Column(columnDefinition="MEDIUMTEXT") private String submissionText;
 @Column(nullable=false) private Instant submittedAt;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private SubmissionStatus status;
 @Column(nullable=false,length=2000) private String attachmentIds;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private User changedBy;
 @Column(nullable=false) private Instant changedAt;
 public AssignmentSubmission getSubmission(){return submission;}
 public void setSubmission(AssignmentSubmission value){submission=value;}
 public long getRevisionNumber(){return revisionNumber;}
 public void setRevisionNumber(long value){revisionNumber=value;}
 public String getSubmissionText(){return submissionText;}
 public void setSubmissionText(String value){submissionText=value;}
 public Instant getSubmittedAt(){return submittedAt;}
 public void setSubmittedAt(Instant value){submittedAt=value;}
 public SubmissionStatus getStatus(){return status;}
 public void setStatus(SubmissionStatus value){status=value;}
 public String getAttachmentIds(){return attachmentIds;}
 public void setAttachmentIds(String value){attachmentIds=value;}
 public User getChangedBy(){return changedBy;}
 public void setChangedBy(User value){changedBy=value;}
 public Instant getChangedAt(){return changedAt;}
 public void setChangedAt(Instant value){changedAt=value;}
}

