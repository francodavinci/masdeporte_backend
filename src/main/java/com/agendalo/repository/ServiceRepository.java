package com.agendalo.repository;

import com.agendalo.domain.BusinessService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<BusinessService, Long> {
}