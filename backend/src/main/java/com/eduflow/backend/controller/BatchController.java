package com.eduflow.backend.controller;
import com.eduflow.backend.dto.*;
import com.eduflow.backend.entity.BatchStatus;
import com.eduflow.backend.service.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/batches")
public class BatchController {
    private final BatchService batches; private final BatchEnrollmentService enrollments; private final BatchTimetableService timetable;
    public BatchController(BatchService batches,BatchEnrollmentService enrollments,BatchTimetableService timetable) { this.batches=batches; this.enrollments=enrollments; this.timetable=timetable; }
    @GetMapping public PageResponse<BatchResponse> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,@RequestParam(defaultValue="") String search,@RequestParam(required=false) Long courseId,@RequestParam(required=false) BatchStatus status) { return batches.list(page,size,search,courseId,status); }
    @GetMapping("/stats") public Map<String,Long> stats() { return batches.stats(); }
    @GetMapping("/my-enrollments") public PageResponse<EnrollmentResponse> mine(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) { return enrollments.mine(page,size); }
    @GetMapping("/{id}") public BatchResponse get(@PathVariable Long id) { return batches.get(id); }
    @PostMapping public ResponseEntity<BatchResponse> create(@Valid @RequestBody BatchRequest r) { var b=batches.create(r); return ResponseEntity.created(URI.create("/api/batches/"+b.id())).body(b); }
    @PutMapping("/{id}") public BatchResponse update(@PathVariable Long id,@Valid @RequestBody BatchRequest r) { return batches.update(id,r); }
    @GetMapping("/{id}/teachers") public List<TeacherAssignmentResponse> teachers(@PathVariable Long id) { return timetable.teachers(id); }
    @PostMapping("/{id}/teachers") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public TeacherAssignmentResponse assign(@PathVariable Long id,@Valid @RequestBody TeacherAssignmentRequest r) { return timetable.assign(id,r); }
    @DeleteMapping("/{id}/teachers/{teacherId}") public TeacherAssignmentResponse unassign(@PathVariable Long id,@PathVariable Long teacherId) { return timetable.unassign(id,teacherId); }
    @GetMapping("/{id}/enrollments") public PageResponse<EnrollmentResponse> roster(@PathVariable Long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) { return enrollments.list(id,page,size); }
    @PostMapping("/{id}/enrollments") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public EnrollmentResponse enroll(@PathVariable Long id,@Valid @RequestBody EnrollmentRequest r) { return enrollments.enroll(id,r); }
    @PatchMapping("/{id}/enrollments/{enrollmentId}/withdraw") public EnrollmentResponse withdraw(@PathVariable Long id,@PathVariable Long enrollmentId,@Valid @RequestBody WithdrawalRequest r) { return enrollments.withdraw(id,enrollmentId,r); }
    @GetMapping("/{id}/schedules") public List<ScheduleResponse> schedules(@PathVariable Long id) { return timetable.schedules(id); }
    @PostMapping("/{id}/schedules") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public ScheduleResponse createSchedule(@PathVariable Long id,@Valid @RequestBody ScheduleRequest r) { return timetable.create(id,r); }
    @PutMapping("/{id}/schedules/{scheduleId}") public ScheduleResponse updateSchedule(@PathVariable Long id,@PathVariable Long scheduleId,@Valid @RequestBody ScheduleRequest r) { return timetable.update(id,scheduleId,r); }
}
