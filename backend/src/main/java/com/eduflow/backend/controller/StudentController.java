package com.eduflow.backend.controller;

import com.eduflow.backend.dto.*;
import com.eduflow.backend.entity.StudentStatus;
import com.eduflow.backend.service.StudentService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService students;

    public StudentController(StudentService students) { this.students = students; }

    @PostMapping
    public ResponseEntity<StudentResponse> add(@Valid @RequestBody StudentRequest request) {
        StudentResponse response = students.addStudent(request);
        return ResponseEntity.created(URI.create("/api/students/" + response.id())).body(response);
    }

    @GetMapping
    public PageResponse<StudentResponse> list(@RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return search.isBlank() && status == null ? students.getAllStudents(page, size)
                : students.searchStudents(search, status, page, size);
    }

    @GetMapping("/me")
    public StudentResponse me() { return students.getMyProfile(); }

    @GetMapping("/{id}")
    public StudentResponse get(@PathVariable Long id) { return students.getStudentById(id); }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return students.updateStudent(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public StudentResponse deactivate(@PathVariable Long id) { return students.deactivateStudent(id); }
}
