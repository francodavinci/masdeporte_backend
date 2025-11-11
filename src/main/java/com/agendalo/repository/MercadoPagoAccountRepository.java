package com.agendalo.repository;

import com.agendalo.domain.MercadoPagoAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MercadoPagoAccountRepository extends JpaRepository<MercadoPagoAccount, Long> {
    MercadoPagoAccount findByCompanyId(Long companyId);
}