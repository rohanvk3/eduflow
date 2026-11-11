package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="exams",uniqueConstraints=@UniqueConstraint(name="uk_exam_code",columnNames="exam_code"),indexes=@Index(name="ix_exam_batch_time",columnList="batch_id,start_date_time"))
public class Exam extends ExamRecordBase {
 @Column(nullable=false) private java.time.LocalDate eligibilityDate;
 public java.time.LocalDate getEligibilityDate(){return eligibilityDate;}
 public void setEligibilityDate(java.time.LocalDate value){eligibilityDate=value;}
 @Column(length=1000) private String cancellationReason;
 public String getCancellationReason(){return cancellationReason;}
 public void setCancellationReason(String value){cancellationReason=value;}
 @Column(nullable=false,length=40) private String examCode;
 @Column(nullable=false,length=200) private String title;
 @Column(length=4000) private String description;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Batch batch;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ExamType examType;
  private Integer durationMinutes;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal totalMarks;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal passingMarks;
 @Column(nullable=false) private Instant startDateTime;
 @Column(nullable=false) private Instant endDateTime;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ExamStatus status=ExamStatus.DRAFT;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private User createdBy;
 @Column(nullable=false) private boolean resultsPublished;
  private Instant resultsPublishedAt;
 @ManyToOne(fetch=FetchType.LAZY,optional=true) @JoinColumn(nullable=true) private User resultsPublishedBy;
 @Version private long version;
 public String getExamCode(){return examCode;}
 public void setExamCode(String value){examCode=value;}
 public String getTitle(){return title;}
 public void setTitle(String value){title=value;}
 public String getDescription(){return description;}
 public void setDescription(String value){description=value;}
 public Batch getBatch(){return batch;}
 public void setBatch(Batch value){batch=value;}
 public ExamType getExamType(){return examType;}
 public void setExamType(ExamType value){examType=value;}
 public Integer getDurationMinutes(){return durationMinutes;}
 public void setDurationMinutes(Integer value){durationMinutes=value;}
 public BigDecimal getTotalMarks(){return totalMarks;}
 public void setTotalMarks(BigDecimal value){totalMarks=value;}
 public BigDecimal getPassingMarks(){return passingMarks;}
 public void setPassingMarks(BigDecimal value){passingMarks=value;}
 public Instant getStartDateTime(){return startDateTime;}
 public void setStartDateTime(Instant value){startDateTime=value;}
 public Instant getEndDateTime(){return endDateTime;}
 public void setEndDateTime(Instant value){endDateTime=value;}
 public ExamStatus getStatus(){return status;}
 public void setStatus(ExamStatus value){status=value;}
 public User getCreatedBy(){return createdBy;}
 public void setCreatedBy(User value){createdBy=value;}
 public boolean getResultsPublished(){return resultsPublished;}
 public void setResultsPublished(boolean value){resultsPublished=value;}
 public Instant getResultsPublishedAt(){return resultsPublishedAt;}
 public void setResultsPublishedAt(Instant value){resultsPublishedAt=value;}
 public User getResultsPublishedBy(){return resultsPublishedBy;}
 public void setResultsPublishedBy(User value){resultsPublishedBy=value;}
 public long getVersion(){return version;}
}

