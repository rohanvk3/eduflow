package com.eduflow.backend.repository;
import com.eduflow.backend.entity.StudyMaterialVersion;
import org.springframework.data.jpa.repository.*;
public interface StudyMaterialVersionRepository extends JpaRepository<StudyMaterialVersion,Long>,JpaSpecificationExecutor<StudyMaterialVersion> {}

