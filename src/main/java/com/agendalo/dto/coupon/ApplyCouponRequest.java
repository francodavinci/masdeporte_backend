package com.agendalo.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponRequest {
    private String couponCode;
    private Long companyId;
    private Double originalAmount;
    private String userEmail;
}