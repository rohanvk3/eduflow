package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="study_material_versions",uniqueConstraints=@UniqueConstraint(columnNames={"material_id","version_number"}))
public class StudyMaterialVersion extends ResourceRecordBase {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private StudyMaterial material;
 @Column(nullable=false) private int versionNumber;
 @Column(nullable=false,length=180) private String originalFileName;
 @Column(nullable=false,unique=true,length=50) private String storageKey;
 @Column(nullable=false,length=150) private String contentType;
 @Column(nullable=false) private long fileSize;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private User uploadedBy;
 @Column(length=2000) private String changeNotes;
 public StudyMaterial getMaterial(){return material;} public void setMaterial(StudyMaterial value){material=value;}
 public int getVersionNumber(){return versionNumber;} public void setVersionNumber(int value){versionNumber=value;}
 public String getOriginalFileName(){return originalFileName;} public void setOriginalFileName(String value){originalFileName=value;}
 public String getStorageKey(){return storageKey;} public void setStorageKey(String value){storageKey=value;}
 public String getContentType(){return contentType;} public void setContentType(String value){contentType=value;}
 public long getFileSize(){return fileSize;} public void setFileSize(long value){fileSize=value;}
 public User getUploadedBy(){return uploadedBy;} public void setUploadedBy(User value){uploadedBy=value;}
 public String getChangeNotes(){return changeNotes;} public void setChangeNotes(String value){changeNotes=value;}
}

