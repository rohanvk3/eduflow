package com.eduflow.backend.repository;

import com.eduflow.backend.entity.NotificationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationJobRepository extends JpaRepository<NotificationJob, Long>, JpaSpecificationExecutor<NotificationJob> {
}

