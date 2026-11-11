package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="resource_categories")
public class ResourceCategory extends ResourceRecordBase {
 @Column(nullable=false,length=100) private String name;
 @Column(nullable=false,unique=true,length=100) private String normalizedName;
 @Column(length=2000) private String description;
 @Column(length=40) private String icon;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private StudentStatus status=StudentStatus.ACTIVE;
 public String getName(){return name;} public void setName(String value){name=value;}
 public String getNormalizedName(){return normalizedName;} public void setNormalizedName(String value){normalizedName=value;}
 public String getDescription(){return description;} public void setDescription(String value){description=value;}
 public String getIcon(){return icon;} public void setIcon(String value){icon=value;}
 public StudentStatus getStatus(){return status;} public void setStatus(StudentStatus value){status=value;}
}

