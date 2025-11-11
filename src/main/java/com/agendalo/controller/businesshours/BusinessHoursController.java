package com.agendalo.controller.businesshours;

import com.agendalo.dto.businesshours.BusinessHoursSetRequestDto;
import com.agendalo.dto.response.ApiResponse;
import com.agendalo.services.businesshours.BusinessHoursService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/provider/business-hours")
@RequiredArgsConstructor
@Slf4j
public class BusinessHoursController {

    private final BusinessHoursService businessHoursService;

    @PostMapping
    public ResponseEntity<ApiResponse> setBusinessHours(@RequestBody BusinessHoursSetRequestDto requestDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String providerEmail = authentication.getName();
        
        log.info("Proveedor {} solicitando establecer horarios comerciales", providerEmail);
        return businessHoursService.setBusinessHours(requestDto, providerEmail);
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getBusinessHours() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String providerEmail = authentication.getName();
        
        log.info("Proveedor {} solicitando consultar horarios comerciales", providerEmail);
        return businessHoursService.getBusinessHours(providerEmail);
    }
} 