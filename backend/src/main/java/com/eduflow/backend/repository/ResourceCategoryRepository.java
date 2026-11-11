package com.eduflow.backend.repository;
import com.eduflow.backend.entity.ResourceCategory;
import org.springframework.data.jpa.repository.*;
public interface ResourceCategoryRepository extends JpaRepository<ResourceCategory,Long>,JpaSpecificationExecutor<ResourceCategory> {}

