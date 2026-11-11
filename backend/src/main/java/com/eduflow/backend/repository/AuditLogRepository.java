package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;import org.springframework.data.jpa.repository.*;
public interface AuditLogRepository extends JpaRepository<AuditLog,Long>,JpaSpecificationExecutor<AuditLog>{}
