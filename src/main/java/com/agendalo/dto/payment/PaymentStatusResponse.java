package com.agendalo.dto.payment;

import lombok.Data;

@Data
public class PaymentStatusResponse {
    private String paymentId;
    private String status;
    private String preferenceId;
    private Double amount;
    private String currency;
    private String description;
    private boolean appointmentCreated;
    private String appointmentId;
} 