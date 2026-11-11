package com.eduflow.backend.dto;

import com.eduflow.backend.entity.Student;
import com.eduflow.backend.entity.StudentStatus;
import java.time.Instant;
import java.time.LocalDate;

public record StudentResponse(Long id, String admissionNumber, String fullName, String email,
        String phone, LocalDate dateOfBirth, String gender, String address, String guardianName,
        String guardianPhone, LocalDate admissionDate, StudentStatus status, Long userId,
        Instant createdAt, Instant updatedAt) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(student.getId(), student.getAdmissionNumber(), student.getFullName(),
                student.getEmail(), student.getPhone(), student.getDateOfBirth(), student.getGender(),
                student.getAddress(), student.getGuardianName(), student.getGuardianPhone(),
                student.getAdmissionDate(), student.getStatus(),
                student.getUser() == null ? null : student.getUser().getId(),
                student.getCreatedAt(), student.getUpdatedAt());
    }
}
