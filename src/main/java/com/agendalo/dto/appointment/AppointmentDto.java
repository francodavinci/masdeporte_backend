package com.agendalo.dto.appointment;

import com.agendalo.domain.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto {
    private Long id;
    private Long serviceId;
    private String serviceName;
    private Long userId;
    private String userEmail;  // Campo añadido
    private Long companyId;    // Campo añadido
    private String companyName; // Campo añadido
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AppointmentStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}