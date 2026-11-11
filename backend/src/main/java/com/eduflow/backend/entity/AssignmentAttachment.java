package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="assignment_attachments")
public class AssignmentAttachment extends AssignmentRecordBase{
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Assignment assignment;
 @Column(nullable=false,length=180) private String originalFileName;
 @Column(nullable=false,length=60) private String storageKey;
 @Column(nullable=false,length=120) private String contentType;
 @Column(nullable=false) private Long fileSize;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private User uploadedBy;
 public Assignment getAssignment(){return assignment;}
 public void setAssignment(Assignment value){assignment=value;}
 public String getOriginalFileName(){return originalFileName;}
 public void setOriginalFileName(String value){originalFileName=value;}
 public String getStorageKey(){return storageKey;}
 public void setStorageKey(String value){storageKey=value;}
 public String getContentType(){return contentType;}
 public void setContentType(String value){contentType=value;}
 public Long getFileSize(){return fileSize;}
 public void setFileSize(Long value){fileSize=value;}
 public User getUploadedBy(){return uploadedBy;}
 public void setUploadedBy(User value){uploadedBy=value;}
}

