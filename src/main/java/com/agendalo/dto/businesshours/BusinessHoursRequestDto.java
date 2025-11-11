package com.agendalo.dto.businesshours;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class BusinessHoursRequestDto {
    @NotNull(message = "El día de la semana es obligatorio")
    private DayOfWeek day; // Cambio de dayOfWeek a day

    @NotNull(message = "La hora de apertura es obligatoria")
    private LocalTime openTime; // Cambio de openingTime a openTime

    @NotNull(message = "La hora de cierre es obligatoria")
    private LocalTime closeTime; // Cambio de closingTime a closeTime

    // getters y setters
}