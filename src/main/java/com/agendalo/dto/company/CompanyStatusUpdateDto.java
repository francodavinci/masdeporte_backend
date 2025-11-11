package com.agendalo.dto.company;

import com.agendalo.domain.enums.CompanyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompanyStatusUpdateDto {
    
    @NotNull(message = "El status es obligatorio")
    private CompanyStatus status;

    private String reason;
}
