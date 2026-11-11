package com.eduflow.backend.repository;

import com.eduflow.backend.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long>, JpaSpecificationExecutor<NotificationPreference> {
}

