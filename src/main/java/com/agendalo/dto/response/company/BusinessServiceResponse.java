package com.agendalo.dto.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessServiceResponse {
    private Long id;
    private String name;
    private String description;
    private Integer durationMinutes;
    private Double price;
}