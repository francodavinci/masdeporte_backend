package com.agendalo.dto.businesshours;

import lombok.Data;
import java.util.List;

@Data
public class BusinessHoursSetRequestDto {
    private List<BusinessHoursRequestDto> businessHours;
} 