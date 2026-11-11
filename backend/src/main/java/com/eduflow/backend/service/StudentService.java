package com.eduflow.backend.service;

import com.eduflow.backend.dto.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.ResourceNotFoundException;
import com.eduflow.backend.exception.StudentConflictException;
import com.eduflow.backend.repository.StudentRepository;
import com.eduflow.backend.repository.UserRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentService {
    private final StudentRepository students;
    private final UserRepository users;

    public StudentService(StudentRepository students, UserRepository users) {
        this.students = students;
        this.users = users;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse addStudent(StudentRequest request) {
        // Random identifiers avoid the race caused by reading MAX(id) before insertion.
        Student student = new Student("EDU-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT));
        apply(student, request);
        return save(student);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public StudentResponse getStudentById(Long id) {
        Student student = find(id);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && (student.getUser() == null || !student.getUser().getEmail().equals(authentication.getName()))) {
            throw new AccessDeniedException("Access denied");
        }
        return StudentResponse.from(student);
    }

    @PreAuthorize("hasRole('STUDENT')")
    public StudentResponse getMyProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return StudentResponse.from(students.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No student profile is linked to your account")));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<StudentResponse> getAllStudents(int page, int size) {
        return searchStudents("", null, page, size);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<StudentResponse> searchStudents(String query, StudentStatus status, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100");
        }
        String search = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (search.length() > 100) { throw new IllegalArgumentException("Search must not exceed 100 characters"); }
        // Escape LIKE wildcards so search text is treated literally.
        String pattern = "%" + search.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        Specification<Student> specification = (root, criteriaQuery, cb) -> {
            var matches = cb.or(cb.like(cb.lower(root.get("fullName")), pattern, '\\'),
                    cb.like(cb.lower(root.get("email")), pattern, '\\'),
                    cb.like(cb.lower(root.get("admissionNumber")), pattern, '\\'),
                    cb.like(root.get("phone"), pattern, '\\'));
            return status == null ? matches : cb.and(matches, cb.equal(root.get("status"), status));
        };
        var result = students.findAll(specification, PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("admissionDate"), Sort.Order.desc("id"))));
        return PageResponse.from(result.map(StudentResponse::from));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse updateStudent(Long id, StudentRequest request) {
        Student student = find(id);
        apply(student, request);
        return save(student);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse deactivateStudent(Long id) {
        Student student = find(id);
        student.deactivate();
        return save(student);
    }

    private Student find(Long id) {
        return students.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private void apply(Student student, StudentRequest request) {
        if (request.admissionDate().isBefore(request.dateOfBirth())) {
            throw new IllegalArgumentException("Admission date must not be before date of birth");
        }
        Long excludedId = student.getId() == null ? 0L : student.getId();
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (students.existsByEmailIgnoreCaseAndIdNot(email, excludedId)) {
            throw new StudentConflictException("Student email is already registered");
        }
        User linkedUser = null;
        if (request.userId() != null) {
            linkedUser = users.findById(request.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("Linked user not found"));
            if (linkedUser.getRole() != Role.STUDENT) {
                throw new IllegalArgumentException("Only a STUDENT account can be linked");
            }
            if (students.existsByUserIdAndIdNot(linkedUser.getId(), excludedId)) {
                throw new StudentConflictException("User account is already linked to a student");
            }
        }
        student.setFullName(request.fullName().strip());
        student.setEmail(email);
        student.setPhone(request.phone());
        student.setDateOfBirth(request.dateOfBirth());
        student.setGender(request.gender() == null ? null : request.gender().strip());
        student.setAddress(request.address().strip());
        student.setGuardianName(request.guardianName().strip());
        student.setGuardianPhone(request.guardianPhone());
        student.setAdmissionDate(request.admissionDate());
        student.setUser(linkedUser);
    }

    private StudentResponse save(Student student) {
        try {
            return StudentResponse.from(students.saveAndFlush(student));
        } catch (DataIntegrityViolationException exception) {
            throw new StudentConflictException("Admission number, student email, or linked user already exists");
        }
    }
}
