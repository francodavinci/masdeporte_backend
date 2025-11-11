package com.agendalo.dto.response.company;

import com.agendalo.dto.service.BusinessServiceResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCompanyResponse {
    private Long id;
    private String name;
    private String category;
    private String address;
    private String phone;
    private Integer minAdvanceDays;
    private Integer maxAdvanceDays;
    private Boolean hasTimeBetweenTurns;
    private Integer minutesBetweenTurns;
    private Integer cancellationHours;
    private Long ownerId;
    private String ownerEmail;
    private List<BusinessHoursResponse> businessHours;
    private List<BusinessServiceResponse> services;
}