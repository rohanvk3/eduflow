package com.eduflow.backend.repository;

import com.eduflow.backend.entity.Student;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsByUserIdAndIdNot(Long userId, Long id);
    Optional<Student> findByUserEmail(String email);
}
