package com.eduflow.backend.repository;
import com.eduflow.backend.entity.Course;
import org.springframework.data.jpa.repository.*;
public interface CourseRepository extends JpaRepository<Course,Long>, JpaSpecificationExecutor<Course> {
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
