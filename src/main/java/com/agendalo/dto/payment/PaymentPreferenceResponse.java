package com.agendalo.dto.payment;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentPreferenceResponse {
    private String preferenceId;
    private String status;
    private Double amount;
    private String currency;
    private LocalDateTime startTime;
    private String notes;
    
    // Información del servicio
    private Long serviceId;
    private String serviceName;
    private String serviceDescription;
    private Integer serviceDuration;
    
    // Información de la empresa
    private Long companyId;
    private String companyName;
    
    // Información del usuario
    private Long userId;
    private String userEmail;
    
    // Información temporal
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Información de la reserva (si existe)
    private Long appointmentId;
    private String appointmentStatus;
} 