package com.agendalo.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessServiceDto {
        @NotBlank(message = "El nombre del servicio es obligatorio")
        private String name;

        private String description;

        @Min(value = 5, message = "La duración mínima del servicio es de 5 minutos")
        @Max(value = 240, message = "La duración máxima del servicio es de 4 horas")
        private Integer durationMinutes;

        @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor que cero")
        @Digits(integer = 8, fraction = 2, message = "El precio debe tener como máximo 8 dígitos enteros y 2 decimales")
        private BigDecimal price;

}