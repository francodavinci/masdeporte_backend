package com.agendalo.repository;

import com.agendalo.domain.PaymentPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentPreferenceRepository extends JpaRepository<PaymentPreference, Long> {
    Optional<PaymentPreference> findByPreferenceId(String preferenceId);
    Optional<PaymentPreference> findByExternalReference(String externalReference);
    List<PaymentPreference> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<PaymentPreference> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
} 