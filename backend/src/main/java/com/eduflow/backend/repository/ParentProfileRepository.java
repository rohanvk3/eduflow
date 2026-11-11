package com.eduflow.backend.repository;
import com.eduflow.backend.entity.ParentProfile;
import org.springframework.data.jpa.repository.*;
public interface ParentProfileRepository extends JpaRepository<ParentProfile,Long>,JpaSpecificationExecutor<ParentProfile> {}

