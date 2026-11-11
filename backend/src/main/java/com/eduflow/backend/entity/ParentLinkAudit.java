package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="parent_link_audits")
public class ParentLinkAudit extends ResourceRecordBase {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private ParentStudentLink link;
 @Enumerated(EnumType.STRING) @Column(length=20) private ParentLinkStatus previousStatus;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ParentLinkStatus newStatus;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private RelationshipType relationshipType;
 @Column(nullable=false) private boolean primaryGuardian;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private User changedBy;
 @Column(nullable=false) private Instant changedAt;
 @Column(nullable=false,length=1000) private String reason;
 public ParentStudentLink getLink(){return link;} public void setLink(ParentStudentLink value){link=value;}
 public ParentLinkStatus getPreviousStatus(){return previousStatus;} public void setPreviousStatus(ParentLinkStatus value){previousStatus=value;}
 public ParentLinkStatus getNewStatus(){return newStatus;} public void setNewStatus(ParentLinkStatus value){newStatus=value;}
 public RelationshipType getRelationshipType(){return relationshipType;} public void setRelationshipType(RelationshipType value){relationshipType=value;}
 public boolean getPrimaryGuardian(){return primaryGuardian;} public void setPrimaryGuardian(boolean value){primaryGuardian=value;}
 public User getChangedBy(){return changedBy;} public void setChangedBy(User value){changedBy=value;}
 public Instant getChangedAt(){return changedAt;} public void setChangedAt(Instant value){changedAt=value;}
 public String getReason(){return reason;} public void setReason(String value){reason=value;}
}

