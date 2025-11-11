package com.agendalo.repository;

import com.agendalo.domain.Appointment;
import com.agendalo.domain.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByUserId(Long userId);

    List<Appointment> findByCompanyId(Long companyId);

    List<Appointment> findByServiceId(Long serviceId);

    List<Appointment> findByUserIdAndStartTimeBetween(
            Long userId, LocalDateTime start, LocalDateTime end);

    List<Appointment> findByCompanyIdAndStartTimeBetween(
            Long companyId, LocalDateTime start, LocalDateTime end);

    List<Appointment> findByCompanyIdAndStartTimeBetweenAndStatusNot(
            Long companyId, LocalDateTime start, LocalDateTime end, AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.company.id = :companyId " +
            "AND a.status != :excludedStatus " +
            "AND ((a.startTime <= :endTime AND a.endTime >= :startTime))")
    List<Appointment> findOverlappingAppointments(
            @Param("companyId") Long companyId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludedStatus") AppointmentStatus excludedStatus);

    @Query("SELECT a FROM Appointment a WHERE a.company.id = :companyId " +
            "AND a.status != :excludedStatus " +
            "AND ((a.startTime <= :endTime AND a.endTime >= :startTime))")
    List<Appointment> findNearbyAppointments(
            @Param("companyId") Long companyId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludedStatus") AppointmentStatus excludedStatus);

    List<Appointment> findByStatusAndStartTimeBefore(
            AppointmentStatus status, LocalDateTime dateTime);

    List<Appointment> findByServiceIdAndUserIdAndStartTime(
            Long serviceId, Long userId, LocalDateTime startTime);

    Optional<Appointment> findByPaymentId(String paymentId);

}