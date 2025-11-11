package com.agendalo.dto.company;

import com.agendalo.domain.enums.CompanyStatus;
import com.agendalo.dto.businesshours.BusinessHoursRequestDto;
import com.agendalo.dto.service.BusinessServiceDto;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRegistrationDto {

    @NotBlank(message = "La categoría es obligatoria")
    private String category;

    @NotBlank(message = "El nombre de la compañía es obligatorio")
    private String companyName;

    @NotBlank(message = "El teléfono es obligatorio")
    private String phone;

    @NotBlank(message = "La dirección es obligatoria")
    private String address;

    @Min(value = 0, message = "Los días mínimos de anticipación no pueden ser negativos")
    @Max(value = 30, message = "Los días mínimos de anticipación no pueden ser más de 30")
    private Integer minAdvanceDays;

    @Min(value = 1, message = "Los días máximos de anticipación deben ser al menos 1")
    @Max(value = 90, message = "Los días máximos de anticipación no pueden ser más de 90")
    private Integer maxAdvanceDays;

    @NotNull(message = "Debe indicar si necesita tiempo entre turnos")
    private Boolean hasTimeBetweenTurns;

    @Min(value = 0, message = "Los minutos entre turnos no pueden ser negativos")
    @Max(value = 120, message = "Los minutos entre turnos no pueden ser más de 120")
    private Integer minutesBetweenTurns;

    @Min(value = 1, message = "Las horas de cancelación deben ser al menos 1")
    @Max(value = 48, message = "Las horas de cancelación no pueden ser más de 48")
    private Integer cancellationHours;

    @NotNull(message = "Los horarios son obligatorios")
    @Size(min = 1, message = "Debe proporcionar al menos un horario")
    private List<BusinessHoursRequestDto> businessHours;

    @NotBlank(message = "El slug de URL es obligatorio")
    @Size(min = 3, max = 50, message = "El slug debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "El slug solo puede contener letras minúsculas, números y guiones")
    private String urlSlug;

    private Long id;

    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;

    private Long logoId; // ID de la imagen del logo
    private String logoUrl; // URL para acceder al logo
    private String ownerName;
    private String ownerEmail;
    private CompanyStatus requestStatus;
    private LocalDateTime registrationDate;
    private byte[] logoData;
}