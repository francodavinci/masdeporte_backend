package com.agendalo.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsageDto {
    private Long id;
    private CouponDto coupon;
    private Double originalAmount;
    private Double discountAmount;
    private Double finalAmount;
    private LocalDateTime usedAt;
}