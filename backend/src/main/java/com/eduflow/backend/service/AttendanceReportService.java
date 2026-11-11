package com.eduflow.backend.service;
import com.eduflow.backend.dto.*;
import com.eduflow.backend.dto.AttendanceDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import com.eduflow.backend.repository.*;
import com.eduflow.backend.repository.AttendanceReportRepository.Filter;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true) @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class AttendanceReportService {
    private final AttendanceReportRepository reports;private final AttendanceSessionService sessions;
    private final AttendanceScope scope;private final StudentRepository students;private final BatchRepository batches;
    public AttendanceReportService(AttendanceReportRepository reports,AttendanceSessionService sessions,AttendanceScope scope,StudentRepository students,BatchRepository batches) {this.reports=reports;this.sessions=sessions;this.scope=scope;this.students=students;this.batches=batches;}
    @PreAuthorize("hasRole('ADMIN')")
    public Report reports(Long courseId,Long batchId,Long studentId,LocalDate from,LocalDate to,int page,int size,int batchPage) {return report(filter(courseId,batchId,studentId,from,to),page,size,batchPage);}
    public Report batch(Long batchId,LocalDate from,LocalDate to,int page,int size) {scope.staffBatch(batchId);if(!batches.existsById(batchId))throw new ResourceNotFoundException("Batch not found");return report(filter(null,batchId,null,from,to),page,size);}
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public Report student(Long studentId,LocalDate from,LocalDate to,int page,int size) {
        Student own=students.findByUserEmail(scope.email()).orElse(null);scope.student(studentId,own);
        if(!students.existsById(studentId))throw new ResourceNotFoundException("Student not found");
        // STUDENT is constrained to its ID; teacher queries retain assigned-batch SQL scoping.
        Filter f=filter(null,null,studentId,from,to);return report(f,page,size);
    }
    @PreAuthorize("hasRole('STUDENT')")
    public Personal mine(LocalDate from,LocalDate to,int page,int size,int batchPage) {
        var dates=range(from,to);CourseService.validatePage(page,size,"");if(page>100000)throw new IllegalArgumentException("Page too large");Student own=students.findByUserEmail(scope.email()).orElse(null);
        if(own==null) return new Personal(false,new Totals(0,0,0,0,0,null,0),new PageResponse<>(List.of(),page,size,0,0),List.of(),new PageResponse<>(List.of(),page,size,0,0),dates[0],dates[1]);
        Filter f=new Filter(null,null,own.getId(),dates[0],dates[1],null);
        CourseService.validatePage(batchPage,size,"");if(batchPage>100000)throw new IllegalArgumentException("Page too large");
        return new Personal(true,reports.totals(f),reports.groups(f,false,batchPage,size),reports.trend(f),reports.history(f,page,size),dates[0],dates[1]);
    }
    public Overview overview(Long batchId,LocalDate from,LocalDate to) {
        if(batchId!=null)scope.staffBatch(batchId);Filter f=filter(null,batchId,null,from,to);
        return new Overview(reports.totals(f),sessions.pending(batchId,f.from(),f.to()),reports.groups(f,false,0,5),sessions.list(batchId,f.from(),f.to(),AttendanceSessionStatus.FINALIZED,0,5,"sessionDate,desc"),f.from(),f.to());
    }
    @PreAuthorize("hasRole('ADMIN')")
    public byte[] export(Long courseId,Long batchId,Long studentId,LocalDate from,LocalDate to) {
        Filter f=filter(courseId,batchId,studentId,from,to);var result=reports.groups(f,true,0,10000);
        if(result.totalElements()>10000)throw new IllegalArgumentException("Export is limited to 10000 students; narrow the filters");
        StringBuilder csv=new StringBuilder("\uFEFFStudent ID,Student name,Admission number,From date,To date,Eligible finalized sessions,Present,Late,Absent,Excused,Attendance percentage\r\n");
        for(var row:result.content()) {var t=row.totals();List<String> values=List.of(row.id().toString(),row.name(),row.code(),f.from().toString(),f.to().toString(),Long.toString(t.totalEligibleFinalizedSessions()),Long.toString(t.presentCount()),Long.toString(t.lateCount()),Long.toString(t.absentCount()),Long.toString(t.excusedCount()),t.attendancePercentage()==null?"":t.attendancePercentage().toPlainString());csv.append(String.join(",",values.stream().map(AttendanceReportService::csvCell).toList())).append("\r\n");}
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
    public static String csvCell(String value) {
        String stripped=value.stripLeading();if(!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0))>=0 || value.startsWith("\t") || value.startsWith("\r"))value="'"+value;
        return "\""+value.replace("\"","\"\"")+"\"";
    }
    private Report report(Filter f,int page,int size) {return report(f,page,size,0);}
    private Report report(Filter f,int page,int size,int batchPage) {CourseService.validatePage(page,size,"");CourseService.validatePage(batchPage,size,"");if(page>100000 || batchPage>100000)throw new IllegalArgumentException("Page too large");return new Report(reports.totals(f),reports.groups(f,true,page,size),reports.groups(f,false,batchPage,size),reports.trend(f),f.from(),f.to());}
    private Filter filter(Long courseId,Long batchId,Long studentId,LocalDate from,LocalDate to) {
        for(Long id:Arrays.asList(courseId,batchId,studentId))if(id!=null && id<1)throw new IllegalArgumentException("IDs must be positive");
        var dates=range(from,to);String teacher=scope.teacher()?scope.email():null;
        return new Filter(courseId,batchId,studentId,dates[0],dates[1],teacher);
    }
    public static LocalDate[] range(LocalDate from,LocalDate to) {LocalDate end=to==null?BatchService.today():to;LocalDate start=from==null?end.minusDays(29):from;if(start.isAfter(end) || java.time.temporal.ChronoUnit.DAYS.between(start,end)>365)throw new IllegalArgumentException("Date range must be ordered and no longer than 366 days");return new LocalDate[]{start,end};}
}
