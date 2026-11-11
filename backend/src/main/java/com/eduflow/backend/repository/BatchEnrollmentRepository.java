package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface BatchEnrollmentRepository extends JpaRepository<BatchEnrollment,Long>, JpaSpecificationExecutor<BatchEnrollment> {
    @Query("select count(distinct e.student.id) from BatchEnrollment e where e.batch.id=:batchId and e.enrollmentDate<=:date and (e.withdrawalDate is null or e.withdrawalDate>:date) and (e.completionDate is null or e.completionDate>=:date)")
    long countEligibleOn(Long batchId,LocalDate date);
    long countByBatchIdAndStatus(Long batchId,EnrollmentStatus status);
    boolean existsByBatchId(Long batchId);
    boolean existsByBatchIdAndStudentUserEmail(Long batchId,String email);
    List<BatchEnrollment> findByBatchIdAndStudentIdOrderByEnrollmentDate(Long batchId,Long studentId);
    List<BatchEnrollment> findByBatchIdAndStatus(Long batchId,EnrollmentStatus status);
    @Query("select e from BatchEnrollment e where e.batch.id=:batchId and e.enrollmentDate<=:date and (e.withdrawalDate is null or e.withdrawalDate>:date) and (e.completionDate is null or e.completionDate>=:date)")
    List<BatchEnrollment> findEligibleOn(Long batchId,LocalDate date);
}
