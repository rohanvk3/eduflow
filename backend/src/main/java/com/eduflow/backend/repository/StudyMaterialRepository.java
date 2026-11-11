package com.eduflow.backend.repository;
import com.eduflow.backend.entity.StudyMaterial;
import org.springframework.data.jpa.repository.*;
public interface StudyMaterialRepository extends JpaRepository<StudyMaterial,Long>,JpaSpecificationExecutor<StudyMaterial> {}

