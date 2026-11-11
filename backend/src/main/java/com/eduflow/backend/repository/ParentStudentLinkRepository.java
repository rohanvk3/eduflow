package com.eduflow.backend.repository;
import com.eduflow.backend.entity.ParentStudentLink;
import org.springframework.data.jpa.repository.*;
public interface ParentStudentLinkRepository extends JpaRepository<ParentStudentLink,Long>,JpaSpecificationExecutor<ParentStudentLink> {}

