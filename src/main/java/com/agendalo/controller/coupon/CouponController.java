package com.agendalo.controller.coupon;

import com.agendalo.dto.coupon.*;
import com.agendalo.dto.response.ApiResponse;
import com.agendalo.services.coupon.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Slf4j
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/company/{companyId}")
    public ResponseEntity<ApiResponse> createCoupon(
            @PathVariable Long companyId,
            @RequestBody CreateCouponRequest request) {
        try {
            log.info("Creando cupón para compañía: {}", companyId);
            CouponDto coupon = couponService.createCoupon(companyId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Cupón creado exitosamente", coupon));
        } catch (Exception e) {
            log.error("Error creando cupón", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error creando cupón: " + e.getMessage()));
        }
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<ApiResponse> getCompanyCoupons(@PathVariable Long companyId) {
        try {
            log.info("Obteniendo cupones para compañía: {}", companyId);
            List<CouponDto> coupons = couponService.getCompanyCoupons(companyId);
            return ResponseEntity.ok(new ApiResponse(true, "Cupones obtenidos exitosamente", coupons));
        } catch (Exception e) {
            log.error("Error obteniendo cupones", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error obteniendo cupones: " + e.getMessage()));
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse> applyCoupon(@RequestBody ApplyCouponRequest request) {
        try {
            log.info("Aplicando cupón: {}", request.getCouponCode());
            ApplyCouponResponse response = couponService.applyCoupon(
                    request.getCouponCode(),
                    request.getCompanyId(),
                    request.getOriginalAmount(),
                    request.getUserEmail()
            );
            return ResponseEntity.ok(new ApiResponse(true, response.getMessage(), response));
        } catch (Exception e) {
            log.error("Error aplicando cupón", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error aplicando cupón: " + e.getMessage()));
        }
    }

    @PostMapping("/use")
    public ResponseEntity<ApiResponse> useCoupon(@RequestBody UseCouponRequest request) {
        try {
            log.info("Usando cupón: {} para turno: {}", request.getCouponCode());
            CouponUsageDto usage = couponService.useCoupon(
                    request.getCouponCode()
            );
            return ResponseEntity.ok(new ApiResponse(true, "Cupón usado exitosamente", usage));
        } catch (Exception e) {
            log.error("Error usando cupón", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error usando cupón: " + e.getMessage()));
        }
    }

    @PostMapping("/apply-and-use")
    public ResponseEntity<ApiResponse> applyAndUseCoupon(@RequestBody ApplyCouponRequest request) {
        try {
            log.info("Aplicando y usando cupón: {}", request.getCouponCode());
            CouponUsageDto usage = couponService.applyAndUseCoupon(
                    request.getCouponCode(),
                    request.getCompanyId(),
                    request.getOriginalAmount(),
                    request.getUserEmail()
            );
            return ResponseEntity.ok(new ApiResponse(true, "Cupón aplicado y usado exitosamente", usage));
        } catch (Exception e) {
            log.error("Error aplicando y usando cupón", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error aplicando y usando cupón: " + e.getMessage()));
        }
    }

    @PutMapping("/{couponId}")
    public ResponseEntity<ApiResponse> updateCoupon(
            @PathVariable Long couponId,
            @RequestBody CreateCouponRequest request) {
        try {
            log.info("Actualizando cupón: {}", couponId);
            CouponDto coupon = couponService.updateCoupon(couponId, request);
            return ResponseEntity.ok(new ApiResponse(true, "Cupón actualizado exitosamente", coupon));
        } catch (Exception e) {
            log.error("Error actualizando cupón", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error actualizando cupón: " + e.getMessage()));
        }
    }


    @DeleteMapping("/{couponId}")
    public ResponseEntity<?> deleteCoupon(@PathVariable Long couponId) {
        try {
            couponService.deleteCoupon(couponId);
            return ResponseEntity.ok().body(Map.of("message", "Cupón eliminado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{couponId}/deactivate")
    public ResponseEntity<?> deactivateCoupon(@PathVariable Long couponId) {
        try {
            couponService.deactivateCoupon(couponId);
            return ResponseEntity.ok().body(Map.of("message", "Cupón desactivado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @PutMapping("/{couponId}/activate")
    public ResponseEntity<?> activateCoupon(@PathVariable Long couponId) {
        try {
            couponService.activateCoupon(couponId);
            return ResponseEntity.ok().body(Map.of("message", "Cupón activado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}