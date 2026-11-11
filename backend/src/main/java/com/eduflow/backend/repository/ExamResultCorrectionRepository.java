package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface ExamResultCorrectionRepository extends JpaRepository<ExamResultCorrection,Long>{
 List<ExamResultCorrection> findByResultIdOrderByCorrectedAt(Long id);
}
