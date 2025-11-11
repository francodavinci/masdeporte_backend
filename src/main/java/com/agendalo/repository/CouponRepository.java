package com.agendalo.repository;

import com.agendalo.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeAndCompanyId(String code, Long companyId);

    List<Coupon> findByCompanyIdAndIsActiveTrue(Long companyId);

    List<Coupon> findByCompanyId(Long companyId);

    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.isActive = true AND c.validFrom <= :now AND c.validUntil >= :now")
    Optional<Coupon> findValidCoupon(@Param("code") String code, @Param("now") LocalDateTime now);

    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.company.id = :companyId AND c.isActive = true AND c.validFrom <= :now AND c.validUntil >= :now")
    Optional<Coupon> findValidCouponForCompany(@Param("code") String code,@Param("companyId") Long companyId, @Param("now") LocalDateTime now);

    @Query("SELECT c FROM Coupon c WHERE c.company.id = :companyId AND c.isActive = true ORDER BY c.createdAt DESC")
    List<Coupon> findActiveCouponsByCompany(@Param("companyId") Long companyId);
}