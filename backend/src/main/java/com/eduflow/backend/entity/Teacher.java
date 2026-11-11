package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "teachers", uniqueConstraints = @UniqueConstraint(name = "uk_teacher_user", columnNames = "user_id"))
public class Teacher {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column(nullable = false, unique = true, length = 50) private String employeeId;
 @Column(nullable = false, length = 100) private String fullName;
 @Column(nullable = false, unique = true, length = 254) private String email;
 @Column(length = 16) private String phone;
 @Column(length = 30) private String gender;
  private LocalDate dateOfBirth;
 @Column(length = 200) private String qualification;
 @Column(length = 200) private String specialization;
  private Integer experienceYears;
  private LocalDate joiningDate;
 @Column(length = 500) private String address;
 @Column(precision = 12, scale = 2) private BigDecimal salary;
 @ElementCollection @CollectionTable(name = "teacher_subjects", joinColumns = @JoinColumn(name = "teacher_id"), uniqueConstraints = @UniqueConstraint(columnNames = {"teacher_id", "subject"}))
 @Column(name = "subject", nullable = false, length = 100) @BatchSize(size = 50)
 private Set<String> subjects = new LinkedHashSet<>();
 @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StudentStatus status = StudentStatus.ACTIVE;
 @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
 @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
 @Column(nullable = false) private LocalDateTime updatedAt;
 public Teacher() {}
 @PrePersist void createTimestamp() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
 @PreUpdate void updateTimestamp() { updatedAt = LocalDateTime.now(); }
 public Long getId() { return id; }
 public Set<String> getSubjects() { return subjects; }
 public void setSubjects(Set<String> subjects) { this.subjects = new LinkedHashSet<>(subjects); }
 public StudentStatus getStatus() { return status; }
 public void setStatus(StudentStatus status) { this.status = status; }
 public User getUser() { return user; }
 public void setUser(User user) { this.user = user; }
 public LocalDateTime getCreatedAt() { return createdAt; }
 public LocalDateTime getUpdatedAt() { return updatedAt; }
 public String getEmployeeId() { return employeeId; }
 public void setEmployeeId(String value) { employeeId = value; }
 public String getFullName() { return fullName; }
 public void setFullName(String value) { fullName = value; }
 public String getEmail() { return email; }
 public void setEmail(String value) { email = value; }
 public String getPhone() { return phone; }
 public void setPhone(String value) { phone = value; }
 public String getGender() { return gender; }
 public void setGender(String value) { gender = value; }
 public LocalDate getDateOfBirth() { return dateOfBirth; }
 public void setDateOfBirth(LocalDate value) { dateOfBirth = value; }
 public String getQualification() { return qualification; }
 public void setQualification(String value) { qualification = value; }
 public String getSpecialization() { return specialization; }
 public void setSpecialization(String value) { specialization = value; }
 public Integer getExperienceYears() { return experienceYears; }
 public void setExperienceYears(Integer value) { experienceYears = value; }
 public LocalDate getJoiningDate() { return joiningDate; }
 public void setJoiningDate(LocalDate value) { joiningDate = value; }
 public String getAddress() { return address; }
 public void setAddress(String value) { address = value; }
 public BigDecimal getSalary() { return salary; }
 public void setSalary(BigDecimal value) { salary = value; }
}

