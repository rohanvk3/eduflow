package com.eduflow.backend.dto;
import com.eduflow.backend.entity.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.Set;
public record TeacherAdminResponse(Long id, String employeeId, String fullName, String email, String phone, String gender, LocalDate dateOfBirth, String qualification, String specialization, Integer experienceYears, LocalDate joiningDate, String address, Set<String> subjects, StudentStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, BigDecimal salary, Long userId) {
 public static TeacherAdminResponse from(Teacher t) { return new TeacherAdminResponse(t.getId(), t.getEmployeeId(), t.getFullName(), t.getEmail(), t.getPhone(), t.getGender(), t.getDateOfBirth(), t.getQualification(), t.getSpecialization(), t.getExperienceYears(), t.getJoiningDate(), t.getAddress(), Set.copyOf(t.getSubjects()), t.getStatus(), t.getCreatedAt(), t.getUpdatedAt(), t.getSalary(), t.getUser() == null ? null : t.getUser().getId()); }
}

