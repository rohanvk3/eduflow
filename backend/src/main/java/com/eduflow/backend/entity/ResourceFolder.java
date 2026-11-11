package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="resource_folders")
public class ResourceFolder extends ResourceRecordBase {
 @Column(nullable=false,length=150) private String name;
 @Column(length=2000) private String description;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Course course;
 @ManyToOne(fetch=FetchType.LAZY) private Batch batch;
 @ManyToOne(fetch=FetchType.LAZY) private ResourceFolder parentFolder;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private User createdBy;
 public String getName(){return name;} public void setName(String value){name=value;}
 public String getDescription(){return description;} public void setDescription(String value){description=value;}
 public Course getCourse(){return course;} public void setCourse(Course value){course=value;}
 public Batch getBatch(){return batch;} public void setBatch(Batch value){batch=value;}
 public ResourceFolder getParentFolder(){return parentFolder;} public void setParentFolder(ResourceFolder value){parentFolder=value;}
 public User getCreatedBy(){return createdBy;} public void setCreatedBy(User value){createdBy=value;}
}

