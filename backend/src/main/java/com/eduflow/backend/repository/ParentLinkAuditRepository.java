package com.eduflow.backend.repository;
import com.eduflow.backend.entity.ParentLinkAudit;
import org.springframework.data.jpa.repository.*;
public interface ParentLinkAuditRepository extends JpaRepository<ParentLinkAudit,Long>,JpaSpecificationExecutor<ParentLinkAudit> {}

