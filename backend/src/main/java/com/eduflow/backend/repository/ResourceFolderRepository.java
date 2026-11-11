package com.eduflow.backend.repository;
import com.eduflow.backend.entity.ResourceFolder;
import org.springframework.data.jpa.repository.*;
public interface ResourceFolderRepository extends JpaRepository<ResourceFolder,Long>,JpaSpecificationExecutor<ResourceFolder> {}

