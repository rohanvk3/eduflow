package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface AssignmentRepository extends JpaRepository<Assignment,Long>,JpaSpecificationExecutor<Assignment>{

}

