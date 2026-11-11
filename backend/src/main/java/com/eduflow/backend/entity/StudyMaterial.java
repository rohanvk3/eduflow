package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="study_materials",indexes={@Index(name="ix_material_scope",columnList="course_id,batch_id,visibility"),@Index(name="ix_material_folder",columnList="folder_id")})
public class StudyMaterial extends ResourceRecordBase {
 @Column(nullable=false,length=180) private String title;
 @Column(columnDefinition="TEXT") private String description;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Course course;
 @ManyToOne(fetch=FetchType.LAZY) private Batch batch;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private ResourceCategory category;
 @ManyToOne(fetch=FetchType.LAZY) private ResourceFolder folder;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private User uploadedBy;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ResourceType resourceType;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ResourceVisibility visibility=ResourceVisibility.DRAFT;
 @Column(length=2000) private String externalUrl;
 @Column(nullable=false) private int currentVersion=0;
  private Integer publishedVersion;
  private Instant publishedAt;
 @Version private long version;
 public String getTitle(){return title;} public void setTitle(String value){title=value;}
 public String getDescription(){return description;} public void setDescription(String value){description=value;}
 public Course getCourse(){return course;} public void setCourse(Course value){course=value;}
 public Batch getBatch(){return batch;} public void setBatch(Batch value){batch=value;}
 public ResourceCategory getCategory(){return category;} public void setCategory(ResourceCategory value){category=value;}
 public ResourceFolder getFolder(){return folder;} public void setFolder(ResourceFolder value){folder=value;}
 public User getUploadedBy(){return uploadedBy;} public void setUploadedBy(User value){uploadedBy=value;}
 public ResourceType getResourceType(){return resourceType;} public void setResourceType(ResourceType value){resourceType=value;}
 public ResourceVisibility getVisibility(){return visibility;} public void setVisibility(ResourceVisibility value){visibility=value;}
 public String getExternalUrl(){return externalUrl;} public void setExternalUrl(String value){externalUrl=value;}
 public int getCurrentVersion(){return currentVersion;} public void setCurrentVersion(int value){currentVersion=value;}
 public Integer getPublishedVersion(){return publishedVersion;} public void setPublishedVersion(Integer value){publishedVersion=value;}
 public Instant getPublishedAt(){return publishedAt;} public void setPublishedAt(Instant value){publishedAt=value;}
 public long getVersion(){return version;} public void setVersion(long value){version=value;}
}

