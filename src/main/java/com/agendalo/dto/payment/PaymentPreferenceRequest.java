package com.agendalo.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class PaymentPreferenceRequest {
    private String title;
    private String description;
    private Double amount;
    private Integer quantity;
    private String currency = "ARS";
    
    // Información de la reserva que se creará después del pago exitoso
    @JsonProperty("serviceId")
    private Long serviceId;
    
    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("companyId")
    private Long companyId;
    
    @JsonProperty("startTime")
    private String startTime; // ISO string format
    
    private String notes;
    
    @JsonProperty("userEmail")
    private String userEmail; // Email del usuario que hace la reserva

    @JsonProperty("external_reference")
    private String externalReference;

    private Map<String, String> backUrls;

    @Override
    public String toString() {
        return "PaymentPreferenceRequest{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", quantity=" + quantity +
                ", currency='" + currency + '\'' +
                ", serviceId=" + serviceId +
                ", userId=" + userId +
                ", startTime='" + startTime + '\'' +
                ", notes='" + notes + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", externalReference='" + externalReference + '\'' +
                '}';
    }
}