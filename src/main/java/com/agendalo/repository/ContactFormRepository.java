package com.agendalo.repository;

import com.agendalo.domain.ContactForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContactFormRepository extends JpaRepository<ContactForm, Long> {

    List<ContactForm> findByRead(Boolean read);

    List<ContactForm> findByEmail(String email);

    @Query("SELECT cf FROM ContactForm cf ORDER BY cf.createdAt DESC")
    List<ContactForm> findAllOrderByCreatedAtDesc();

    @Query("SELECT cf FROM ContactForm cf WHERE cf.createdAt BETWEEN :startDate AND :endDate ORDER BY cf.createdAt DESC")
    List<ContactForm> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);

    long countByRead(Boolean read);
}
