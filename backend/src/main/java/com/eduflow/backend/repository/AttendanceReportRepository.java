package com.eduflow.backend.repository;

import com.eduflow.backend.dto.*;
import com.eduflow.backend.dto.AttendanceDtos.*;
import com.eduflow.backend.entity.*;
import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Repository;

/** Aggregates in SQL rather than loading entire attendance tables into memory. */
@Repository
public class AttendanceReportRepository {
    private final EntityManager em;
    public AttendanceReportRepository(EntityManager em) { this.em=em; }
    public record Filter(Long courseId,Long batchId,Long studentId,LocalDate from,LocalDate to,String teacherEmail) {}
    private static final String COUNTS="count(r),sum(case when r.status=com.eduflow.backend.entity.AttendanceStatus.PRESENT then 1 else 0 end),sum(case when r.status=com.eduflow.backend.entity.AttendanceStatus.LATE then 1 else 0 end),sum(case when r.status=com.eduflow.backend.entity.AttendanceStatus.ABSENT then 1 else 0 end),sum(case when r.status=com.eduflow.backend.entity.AttendanceStatus.EXCUSED then 1 else 0 end),count(distinct r.session.id)";
    private String where(Filter f) {
        return " from AttendanceRecord r where r.session.status=com.eduflow.backend.entity.AttendanceSessionStatus.FINALIZED and r.session.sessionDate between :fromDate and :toDate"
            +(f.courseId()!=null?" and r.session.batch.course.id=:courseId":"")+(f.batchId()!=null?" and r.session.batch.id=:batchId":"")+(f.studentId()!=null?" and r.student.id=:studentId":"")
            +(f.teacherEmail()!=null?" and exists (select a.id from BatchTeacherAssignment a where a.batch=r.session.batch and a.active=true and a.teacher.user.email=:teacherEmail)":"");
    }
    private Query query(String select,Filter f,String tail) {
        Query q=em.createQuery(select+where(f)+tail).setParameter("fromDate",f.from()).setParameter("toDate",f.to());
        if(f.courseId()!=null)q.setParameter("courseId",f.courseId());if(f.batchId()!=null)q.setParameter("batchId",f.batchId());if(f.studentId()!=null)q.setParameter("studentId",f.studentId());if(f.teacherEmail()!=null)q.setParameter("teacherEmail",f.teacherEmail());return q;
    }
    public Totals totals(Filter f) {
        Totals t=counts((Object[])query("select "+COUNTS,f,"").getSingleResult(),0);
        String jpql="select count(s) from AttendanceSession s where s.status=com.eduflow.backend.entity.AttendanceSessionStatus.FINALIZED and s.sessionDate between :fromDate and :toDate"
            +(f.courseId()!=null?" and s.batch.course.id=:courseId":"")+(f.batchId()!=null?" and s.batch.id=:batchId":"")
            +(f.studentId()!=null?" and exists(select r.id from AttendanceRecord r where r.session=s and r.student.id=:studentId)":"")
            +(f.teacherEmail()!=null?" and exists(select a.id from BatchTeacherAssignment a where a.batch=s.batch and a.active=true and a.teacher.user.email=:teacherEmail)":"");
        Query q=em.createQuery(jpql).setParameter("fromDate",f.from()).setParameter("toDate",f.to());
        if(f.courseId()!=null)q.setParameter("courseId",f.courseId());if(f.batchId()!=null)q.setParameter("batchId",f.batchId());if(f.studentId()!=null)q.setParameter("studentId",f.studentId());if(f.teacherEmail()!=null)q.setParameter("teacherEmail",f.teacherEmail());
        return new Totals(t.totalEligibleFinalizedSessions(),t.presentCount(),t.lateCount(),t.absentCount(),t.excusedCount(),t.attendancePercentage(),((Number)q.getSingleResult()).longValue());
    }
    public PageResponse<GroupSummary> groups(Filter f,boolean byStudent,int page,int size) {
        String id=byStudent?"r.student.id":"r.session.batch.id",name=byStudent?"r.student.fullName":"r.session.batch.name",code=byStudent?"r.student.admissionNumber":"r.session.batch.code";
        long total=((Number)query("select count(distinct "+id+")",f,"").getSingleResult()).longValue();
        @SuppressWarnings("unchecked") List<Object[]> rows=query("select "+id+","+name+","+code+","+COUNTS,f," group by "+id+","+name+","+code+" order by "+name+","+id).setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size).getResultList();
        return new PageResponse<>(rows.stream().map(r->new GroupSummary(((Number)r[0]).longValue(),(String)r[1],(String)r[2],counts(r,3))).toList(),page,size,total,(int)((total+size-1)/size));
    }
    public List<Trend> trend(Filter f) {
        @SuppressWarnings("unchecked") List<Object[]> rows=query("select r.session.sessionDate,"+COUNTS,f," group by r.session.sessionDate order by r.session.sessionDate").getResultList();
        return rows.stream().map(r->new Trend((LocalDate)r[0],counts(r,1))).toList();
    }
    public PageResponse<History> history(Filter f,int page,int size) {
        long total=((Number)query("select count(r)",f,"").getSingleResult()).longValue();
        @SuppressWarnings("unchecked") List<AttendanceRecord> rows=query("select r",f," order by r.session.sessionDate desc,r.session.startTime desc,r.id desc").setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size).getResultList();
        return new PageResponse<>(rows.stream().map(r->{var s=r.getSession();return new History(s.getId(),s.getBatch().getId(),s.getBatch().getName(),s.getBatch().getCourse().getName(),s.getSessionDate(),s.getStartTime(),s.getEndTime(),s.getTopic(),r.getStatus(),r.getRemarks());}).toList(),page,size,total,(int)((total+size-1)/size));
    }
    private Totals counts(Object[] row,int offset) {
        long total=n(row[offset]),present=n(row[offset+1]),late=n(row[offset+2]),absent=n(row[offset+3]),excused=n(row[offset+4]),sessions=n(row[offset+5]);
        long denominator=present+late+absent;
        BigDecimal percentage=denominator==0?null:BigDecimal.valueOf(present+late).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominator),2,RoundingMode.HALF_UP);
        return new Totals(total,present,late,absent,excused,percentage,sessions);
    }
    private long n(Object value) { return value==null?0:((Number)value).longValue(); }
}
