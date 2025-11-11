package com.agendalo.repository;

import com.agendalo.domain.BusinessService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessServiceRepository extends JpaRepository<BusinessService, Long> {

    List<BusinessService> findByCompanyId(Long companyId);

    boolean existsByIdAndCompanyId(Long id, Long companyId);

}