package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="assignment_recipients",uniqueConstraints=@UniqueConstraint(name="uk_assignment_recipient",columnNames={"assignment_id","student_id"}))
public class AssignmentRecipient extends AssignmentRecordBase{
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Assignment assignment;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Student student;
 public Assignment getAssignment(){return assignment;}
 public void setAssignment(Assignment value){assignment=value;}
 public Student getStudent(){return student;}
 public void setStudent(Student value){student=value;}
}

