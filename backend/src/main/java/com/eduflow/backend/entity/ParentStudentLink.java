package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="parent_student_links",uniqueConstraints=@UniqueConstraint(name="uk_parent_child",columnNames={"parent_id","student_id"}),indexes=@Index(name="ix_parent_link_access",columnList="parent_id,status,student_id"))
public class ParentStudentLink extends ResourceRecordBase {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private ParentProfile parent;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Student student;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private RelationshipType relationshipType;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ParentLinkStatus status=ParentLinkStatus.PENDING;
 @Column(nullable=false) private boolean primaryGuardian=false;
 @ManyToOne(fetch=FetchType.LAZY) private User verifiedBy;
  private Instant verifiedAt;
 @Version private long version;
 public ParentProfile getParent(){return parent;} public void setParent(ParentProfile value){parent=value;}
 public Student getStudent(){return student;} public void setStudent(Student value){student=value;}
 public RelationshipType getRelationshipType(){return relationshipType;} public void setRelationshipType(RelationshipType value){relationshipType=value;}
 public ParentLinkStatus getStatus(){return status;} public void setStatus(ParentLinkStatus value){status=value;}
 public boolean getPrimaryGuardian(){return primaryGuardian;} public void setPrimaryGuardian(boolean value){primaryGuardian=value;}
 public User getVerifiedBy(){return verifiedBy;} public void setVerifiedBy(User value){verifiedBy=value;}
 public Instant getVerifiedAt(){return verifiedAt;} public void setVerifiedAt(Instant value){verifiedAt=value;}
 public long getVersion(){return version;} public void setVersion(long value){version=value;}
}

