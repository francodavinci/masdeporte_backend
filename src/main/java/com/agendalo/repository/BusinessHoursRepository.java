package com.agendalo.repository;

import com.agendalo.domain.BusinessHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessHoursRepository extends JpaRepository<BusinessHours, Long> {

    List<BusinessHours> findByCompanyId(Long companyId);

    List<BusinessHours> findByCompanyIdAndDayOfWeek(Long companyId, DayOfWeek dayOfWeek);

    Optional<BusinessHours> findByCompanyIdAndDayOfWeekAndWorkingDayIsTrue(
            Long companyId, DayOfWeek dayOfWeek);
}