package com.agendalo.dto.comment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentRequestDto {
    
    @NotNull(message = "El ID de la compañía es obligatorio")
    private Long companyId;

    @NotNull(message = "Los puntos son obligatorios")
    @Min(value = 1, message = "Los puntos deben ser entre 1 y 5")
    @Max(value = 5, message = "Los puntos deben ser entre 1 y 5")
    private Integer points;

    @NotBlank(message = "La descripción es obligatoria")
    private String description;
} 