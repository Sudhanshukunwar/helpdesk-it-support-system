package com.helpdesk.repository;

import com.helpdesk.entity.Priority;
import com.helpdesk.entity.SlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Priority> {}
