package com.eduflow.backend.service;

import com.eduflow.backend.dto.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import com.eduflow.backend.repository.*;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
public class TeacherService {
    private final TeacherRepository teachers;
    private final UserRepository users;
    public TeacherService(TeacherRepository teachers, UserRepository users) { this.teachers = teachers; this.users = users; }

    @Transactional
    public TeacherAdminResponse createTeacher(TeacherRequest request) {
        Teacher teacher = new Teacher(); apply(teacher, request); return save(teacher);
    }
    public TeacherAdminResponse getTeacherById(Long id) { return TeacherAdminResponse.from(find(id)); }

    public PageResponse<TeacherAdminResponse> getAllTeachers(int page, int size, String sort, String search, StudentStatus status) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Page must be non-negative and size must be 1–100");
        if (search.length() > 100) throw new IllegalArgumentException("Search must not exceed 100 characters");
        String[] ordering = sort.split(",", -1);
        if (ordering.length != 2 || !Set.of("fullName", "employeeId", "email", "joiningDate", "createdAt", "experienceYears", "status").contains(ordering[0]) || !Set.of("asc", "desc").contains(ordering[1]))
            throw new IllegalArgumentException("Invalid sort; use a supported field followed by ,asc or ,desc");
        String pattern = "%" + search.strip().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        Specification<Teacher> spec = (root, query, cb) -> {
            var match = cb.or(cb.like(cb.lower(root.get("fullName")), pattern, '\\'), cb.like(cb.lower(root.get("employeeId")), pattern, '\\'), cb.like(cb.lower(root.get("email")), pattern, '\\'), cb.like(cb.lower(root.get("specialization")), pattern, '\\'));
            return status == null ? match : cb.and(match, cb.equal(root.get("status"), status));
        };
        return PageResponse.from(teachers.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(ordering[1]), ordering[0]).and(Sort.by("id")))).map(TeacherAdminResponse::from));
    }

    @Transactional
    public TeacherAdminResponse updateTeacher(Long id, TeacherRequest request) { Teacher teacher = find(id); apply(teacher, request); return save(teacher); }
    @Transactional
    public TeacherAdminResponse deactivateTeacher(Long id) { Teacher t = find(id); t.setStatus(StudentStatus.INACTIVE); return save(t); }
    @Transactional
    public TeacherAdminResponse activateTeacher(Long id) { Teacher t = find(id); t.setStatus(StudentStatus.ACTIVE); return save(t); }
    public TeacherStatistics getTeacherStatistics() {
        long total = teachers.count(); long active = teachers.countByStatus(StudentStatus.ACTIVE);
        return new TeacherStatistics(total, active, total - active);
    }

    @PreAuthorize("hasRole('TEACHER')")
    public TeacherProfileResponse getMyProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return TeacherProfileResponse.from(teachers.findByUserEmail(email).orElseThrow(() -> new ResourceNotFoundException("No teacher profile is linked to your account. Contact your administrator.")));
    }

    private Teacher find(Long id) { return teachers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Teacher not found")); }
    private void apply(Teacher t, TeacherRequest r) {
        Long excluded = t.getId() == null ? 0L : t.getId();
        String employee = r.employeeId().strip().toUpperCase(Locale.ROOT);
        String email = r.email().strip().toLowerCase(Locale.ROOT);
        if (teachers.existsByEmployeeIdIgnoreCaseAndIdNot(employee, excluded)) throw new TeacherConflictException("Employee ID is already registered");
        if (teachers.existsByEmailIgnoreCaseAndIdNot(email, excluded)) throw new TeacherConflictException("Teacher email is already registered");
        if (r.userId() != null) {
            User user = users.findById(r.userId()).orElseThrow(() -> new ResourceNotFoundException("Linked user not found"));
            if (user.getRole() != Role.TEACHER) throw new IllegalArgumentException("Only a TEACHER account can be linked");
            if (teachers.existsByUserIdAndIdNot(user.getId(), excluded)) throw new TeacherConflictException("User is already linked to another teacher");
            t.setUser(user);
        }
        t.setEmployeeId(employee); t.setEmail(email); t.setFullName(r.fullName().strip());
        if (r.phone() != null) t.setPhone(r.phone());
        if (r.gender() != null) t.setGender(r.gender().strip());
        if (r.dateOfBirth() != null) t.setDateOfBirth(r.dateOfBirth());
        if (r.qualification() != null) t.setQualification(r.qualification().strip());
        if (r.specialization() != null) t.setSpecialization(r.specialization().strip());
        if (r.subjects() != null) t.setSubjects(new LinkedHashSet<>(r.subjects().stream().map(String::strip).toList()));
        if (r.experienceYears() != null) t.setExperienceYears(r.experienceYears());
        if (r.joiningDate() != null) t.setJoiningDate(r.joiningDate());
        if (r.address() != null) t.setAddress(r.address().strip());
        if (r.salary() != null) t.setSalary(r.salary());
        if (t.getDateOfBirth() != null && t.getJoiningDate() != null && t.getJoiningDate().isBefore(t.getDateOfBirth())) throw new IllegalArgumentException("Joining date cannot be before birth date");
    }
    private TeacherAdminResponse save(Teacher teacher) {
        try { return TeacherAdminResponse.from(teachers.saveAndFlush(teacher)); }
        catch (DataIntegrityViolationException exception) { throw new TeacherConflictException("Employee ID, teacher email, or linked user already exists"); }
    }
}
