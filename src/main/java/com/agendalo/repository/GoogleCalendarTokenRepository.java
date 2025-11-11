package com.agendalo.repository;


import com.agendalo.domain.GoogleCalendarToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GoogleCalendarTokenRepository extends JpaRepository<GoogleCalendarToken, Long> {
    Optional<GoogleCalendarToken> findByCompanyId(Long companyId);
    void deleteByCompanyId(Long companyId);
}