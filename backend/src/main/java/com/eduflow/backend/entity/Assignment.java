package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="assignments",indexes=@Index(name="ix_assignment_batch_due",columnList="batch_id,due_date_time"))
public class Assignment extends AssignmentRecordBase{
 @Column(nullable=false,length=200) private String title;
 @Column(columnDefinition="TEXT") private String description;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Batch batch;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private User createdBy;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal maxMarks;
  private Instant publishedAt;
 @Column(nullable=false) private Instant dueDateTime;
 @Column(nullable=false) private boolean allowLateSubmission;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private AssignmentStatus status=AssignmentStatus.DRAFT;
 @Version private long version;
 public String getTitle(){return title;}
 public void setTitle(String value){title=value;}
 public String getDescription(){return description;}
 public void setDescription(String value){description=value;}
 public Batch getBatch(){return batch;}
 public void setBatch(Batch value){batch=value;}
 public User getCreatedBy(){return createdBy;}
 public void setCreatedBy(User value){createdBy=value;}
 public BigDecimal getMaxMarks(){return maxMarks;}
 public void setMaxMarks(BigDecimal value){maxMarks=value;}
 public Instant getPublishedAt(){return publishedAt;}
 public void setPublishedAt(Instant value){publishedAt=value;}
 public Instant getDueDateTime(){return dueDateTime;}
 public void setDueDateTime(Instant value){dueDateTime=value;}
 public boolean getAllowLateSubmission(){return allowLateSubmission;}
 public void setAllowLateSubmission(boolean value){allowLateSubmission=value;}
 public AssignmentStatus getStatus(){return status;}
 public void setStatus(AssignmentStatus value){status=value;}
 public long getVersion(){return version;}
}

