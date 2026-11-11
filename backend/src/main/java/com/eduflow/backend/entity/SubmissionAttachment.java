package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="submission_attachments")
public class SubmissionAttachment extends AssignmentRecordBase{
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private AssignmentSubmission submission;
 @Column(nullable=false,length=180) private String originalFileName;
 @Column(nullable=false,length=60) private String storageKey;
 @Column(nullable=false,length=120) private String contentType;
 @Column(nullable=false) private Long fileSize;
 @Column(nullable=false) private boolean active=true;
 public AssignmentSubmission getSubmission(){return submission;}
 public void setSubmission(AssignmentSubmission value){submission=value;}
 public String getOriginalFileName(){return originalFileName;}
 public void setOriginalFileName(String value){originalFileName=value;}
 public String getStorageKey(){return storageKey;}
 public void setStorageKey(String value){storageKey=value;}
 public String getContentType(){return contentType;}
 public void setContentType(String value){contentType=value;}
 public Long getFileSize(){return fileSize;}
 public void setFileSize(Long value){fileSize=value;}
 public boolean getActive(){return active;}
 public void setActive(boolean value){active=value;}
}

