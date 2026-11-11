package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="parent_profiles")
public class ParentProfile extends ResourceRecordBase {
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false,unique=true) private User user;
 @Column(nullable=false,length=16) private String phoneNumber;
 @Column(length=16) private String alternatePhone;
 @Column(length=254) private String contactEmail;
 @Column(length=500) private String address;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private StudentStatus status=StudentStatus.ACTIVE;
 @Version private long version;
 public User getUser(){return user;} public void setUser(User value){user=value;}
 public String getPhoneNumber(){return phoneNumber;} public void setPhoneNumber(String value){phoneNumber=value;}
 public String getAlternatePhone(){return alternatePhone;} public void setAlternatePhone(String value){alternatePhone=value;}
 public String getContactEmail(){return contactEmail;} public void setContactEmail(String value){contactEmail=value;}
 public String getAddress(){return address;} public void setAddress(String value){address=value;}
 public StudentStatus getStatus(){return status;} public void setStatus(StudentStatus value){status=value;}
 public long getVersion(){return version;} public void setVersion(long value){version=value;}
}

