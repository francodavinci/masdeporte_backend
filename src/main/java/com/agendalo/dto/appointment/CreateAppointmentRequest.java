package com.agendalo.dto.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateAppointmentRequest {
    @NotNull(message = "El ID del servicio es obligatorio")
    private Long serviceId;

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long userId;

    @NotNull(message = "La fecha y hora de inicio son obligatorias")
    @Future(message = "La fecha y hora de inicio deben ser futuras")
    private LocalDateTime startTime;

    private String notes;

    private String couponCode;
}