package com.agendalo.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponResponse {
    private Boolean valid;
    private String message;
    private Double discountAmount;
    private Double finalAmount;
    private CouponDto coupon;
}