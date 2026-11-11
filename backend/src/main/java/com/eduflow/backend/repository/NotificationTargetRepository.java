package com.eduflow.backend.repository;

import com.eduflow.backend.entity.NotificationTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationTargetRepository extends JpaRepository<NotificationTarget, Long>, JpaSpecificationExecutor<NotificationTarget> {
}

