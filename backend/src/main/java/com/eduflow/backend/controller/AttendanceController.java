package com.eduflow.backend.controller;

import com.eduflow.backend.dto.*;
import com.eduflow.backend.dto.AttendanceDtos.*;
import com.eduflow.backend.entity.AttendanceSessionStatus;
import com.eduflow.backend.service.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceSessionService sessions;private final AttendanceReportService reports;
    public AttendanceController(AttendanceSessionService sessions,AttendanceReportService reports) {this.sessions=sessions;this.reports=reports;}
    @PostMapping("/sessions") public ResponseEntity<SessionDetails> create(@Valid @RequestBody CreateSession request) {var s=sessions.create(request);return ResponseEntity.created(URI.create("/api/attendance/sessions/"+s.session().id())).body(s);}
    @GetMapping("/sessions") public PageResponse<SessionSummary> list(@RequestParam(required=false) Long batchId,@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate,@RequestParam(required=false) AttendanceSessionStatus status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,@RequestParam(defaultValue="sessionDate,desc") String sort) {return sessions.list(batchId,fromDate,toDate,status,page,size,sort);}
    @GetMapping("/sessions/{id}") public SessionDetails get(@PathVariable Long id) {return sessions.get(id);}
    @PutMapping("/sessions/{id}/records") public SessionDetails save(@PathVariable Long id,@Valid @RequestBody SaveRecords request) {return sessions.save(id,request);}
    @PostMapping("/sessions/{id}/finalize") public SessionDetails finalizeSession(@PathVariable Long id,@Valid @RequestBody(required=false) FinalizeRequest request) {return sessions.finalizeSession(id,request);}
    @GetMapping("/batches/{batchId}/register") public PageResponse<SessionSummary> register(@PathVariable Long batchId,@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate,@RequestParam(required=false) AttendanceSessionStatus status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,@RequestParam(defaultValue="sessionDate,desc") String sort) {return sessions.list(batchId,fromDate,toDate,status,page,size,sort);}
    @GetMapping("/batches/{batchId}/summary") public Report batch(@PathVariable Long batchId,@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) {return reports.batch(batchId,fromDate,toDate,page,size);}
    @GetMapping("/students/{studentId}/summary") public Report student(@PathVariable Long studentId,@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) {return reports.student(studentId,fromDate,toDate,page,size);}
    @GetMapping("/me") public Personal mine(@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,@RequestParam(defaultValue="0") int batchPage) {return reports.mine(fromDate,toDate,page,size,batchPage);}
    @GetMapping("/overview") public Overview overview(@RequestParam(required=false) Long batchId,@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate) {return reports.overview(batchId,fromDate,toDate);}
    @GetMapping("/reports") public Report report(@RequestParam(required=false) Long courseId,@RequestParam(required=false) Long batchId,@RequestParam(required=false) Long studentId,@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,@RequestParam(defaultValue="0") int batchPage) {return reports.reports(courseId,batchId,studentId,fromDate,toDate,page,size,batchPage);}
    @GetMapping("/reports/export") public ResponseEntity<byte[]> export(@RequestParam(required=false) Long courseId,@RequestParam(required=false) Long batchId,@RequestParam(required=false) Long studentId,@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate) {
        byte[] csv=reports.export(courseId,batchId,studentId,fromDate,toDate);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=eduflow-attendance.csv").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(csv);
    }
}
