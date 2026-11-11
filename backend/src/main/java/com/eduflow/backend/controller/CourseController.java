package com.eduflow.backend.controller;
import com.eduflow.backend.dto.*;
import com.eduflow.backend.entity.StudentStatus;
import com.eduflow.backend.service.CourseService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/courses")
public class CourseController {
    private final CourseService courses;
    public CourseController(CourseService courses) { this.courses=courses; }
    @GetMapping public PageResponse<CourseResponse> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,@RequestParam(defaultValue="") String search,@RequestParam(required=false) StudentStatus status) { return courses.list(page,size,search,status); }
    @GetMapping("/{id}") public CourseResponse get(@PathVariable Long id) { return courses.get(id); }
    @PostMapping public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest r) { var c=courses.create(r); return ResponseEntity.created(URI.create("/api/courses/"+c.id())).body(c); }
    @PutMapping("/{id}") public CourseResponse update(@PathVariable Long id,@Valid @RequestBody CourseRequest r) { return courses.update(id,r); }
    @PatchMapping("/{id}/deactivate") public CourseResponse deactivate(@PathVariable Long id) { return courses.status(id,StudentStatus.INACTIVE); }
    @PatchMapping("/{id}/activate") public CourseResponse activate(@PathVariable Long id) { return courses.status(id,StudentStatus.ACTIVE); }
}
