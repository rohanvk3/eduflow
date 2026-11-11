package com.eduflow.backend.service;
import com.eduflow.backend.dto.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import com.eduflow.backend.repository.*;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
@PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
public class CourseService {
    private final CourseRepository courses;
    private final AcademicAccess access;
    public CourseService(CourseRepository courses,AcademicAccess access) { this.courses=courses; this.access=access; }
    public PageResponse<CourseResponse> list(int page,int size,String search,StudentStatus status) {
        validatePage(page,size,search);
        StudentStatus filter=access.hasRole("ADMIN")?status:StudentStatus.ACTIVE;
        String pattern=pattern(search);
        Specification<Course> spec=(root,q,cb)-> {
            var match=cb.or(cb.like(cb.lower(root.get("name")),pattern,'\\'),cb.like(cb.lower(root.get("code")),pattern,'\\'));
            return filter==null?match:cb.and(match,cb.equal(root.get("status"),filter));
        };
        return PageResponse.from(courses.findAll(spec,PageRequest.of(page,size,Sort.by("name").and(Sort.by("id")))).map(this::response));
    }
    public CourseResponse get(Long id) {
        Course c=find(id);
        if (!access.hasRole("ADMIN") && c.getStatus()!=StudentStatus.ACTIVE) throw new AccessDeniedException("Course is inactive");
        return response(c);
    }
    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse create(CourseRequest r) { return save(new Course(),r); }
    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse update(Long id,CourseRequest r) { return save(find(id),r); }
    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse status(Long id,StudentStatus status) { Course c=find(id); c.setStatus(status); return response(courses.saveAndFlush(c)); }
    private Course find(Long id) { return courses.findById(id).orElseThrow(()->new ResourceNotFoundException("Course not found")); }
    private CourseResponse save(Course c,CourseRequest r) {
        String code=r.code().toUpperCase(Locale.ROOT);
        if (courses.existsByCodeIgnoreCaseAndIdNot(code,c.getId()==null?0L:c.getId())) throw new AcademicConflictException("Course code already exists");
        c.setCode(code); c.setName(r.name().strip()); c.setDescription(r.description()); c.setDurationWeeks(r.durationWeeks());
        try { return response(courses.saveAndFlush(c)); } catch (DataIntegrityViolationException e) { throw new AcademicConflictException("Course code already exists"); }
    }
    private CourseResponse response(Course c) { return new CourseResponse(c.getId(),c.getCode(),c.getName(),c.getDescription(),c.getDurationWeeks(),c.getStatus()); }
    public static void validatePage(int page,int size,String search) { if(page<0 || size<1 || size>100 || search.length()>100) throw new IllegalArgumentException("Page must be non-negative, size 1–100, search at most 100 characters"); }
    public static String pattern(String search) { return "%"+search.strip().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%"; }
}
