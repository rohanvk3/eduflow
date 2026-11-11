package com.eduflow.backend.controller;
import com.eduflow.backend.dto.*;
import com.eduflow.backend.entity.StudentStatus;
import com.eduflow.backend.service.TeacherService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {
    private final TeacherService teachers;
    public TeacherController(TeacherService teachers) { this.teachers = teachers; }
    @PostMapping public ResponseEntity<TeacherAdminResponse> create(@Valid @RequestBody TeacherRequest request) {
        var teacher = teachers.createTeacher(request); return ResponseEntity.created(URI.create("/api/teachers/" + teacher.id())).body(teacher);
    }
    @GetMapping public PageResponse<TeacherAdminResponse> list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size, @RequestParam(defaultValue="createdAt,desc") String sort, @RequestParam(defaultValue="") String search, @RequestParam(required=false) StudentStatus status) { return teachers.getAllTeachers(page, size, sort, search, status); }
    @GetMapping("/stats") public TeacherStatistics stats() { return teachers.getTeacherStatistics(); }
    @GetMapping("/me") public TeacherProfileResponse me() { return teachers.getMyProfile(); }
    @GetMapping("/{id}") public TeacherAdminResponse get(@PathVariable Long id) { return teachers.getTeacherById(id); }
    @PutMapping("/{id}") public TeacherAdminResponse update(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) { return teachers.updateTeacher(id, request); }
    @PatchMapping("/{id}/deactivate") public TeacherAdminResponse deactivate(@PathVariable Long id) { return teachers.deactivateTeacher(id); }
    @PatchMapping("/{id}/activate") public TeacherAdminResponse activate(@PathVariable Long id) { return teachers.activateTeacher(id); }
}
