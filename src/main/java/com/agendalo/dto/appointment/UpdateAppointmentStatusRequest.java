package com.agendalo.dto.appointment;


import com.agendalo.domain.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAppointmentStatusRequest {
    @NotNull(message = "El estado es obligatorio")
    private AppointmentStatus status;
}
