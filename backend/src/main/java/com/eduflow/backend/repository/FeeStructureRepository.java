package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.*;
public interface FeeStructureRepository extends JpaRepository<FeeStructure,Long>, JpaSpecificationExecutor<FeeStructure> {
    @Query("select count(f) from FeeStructure f where f.course.id=:courseId and f.id<>:exclude and f.status=com.eduflow.backend.entity.StudentStatus.ACTIVE and (:until is null or f.effectiveFrom<=:until) and (f.effectiveTo is null or f.effectiveTo>=:start)")
    long overlapping(Long courseId,Long exclude,LocalDate start,LocalDate until);
}
