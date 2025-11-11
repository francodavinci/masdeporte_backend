package com.agendalo.repository;

import com.agendalo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailWithDetails(@Param("email") String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.companies WHERE u.id = :userId")
    Optional<User> findByIdWithCompanies(@Param("userId") Long userId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.appointments WHERE u.id = :userId")
    Optional<User> findByIdWithAppointments(@Param("userId") Long userId);
}