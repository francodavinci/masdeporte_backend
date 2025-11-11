package com.agendalo.dto.company;

import com.agendalo.domain.Company;
import com.agendalo.domain.BusinessHours;
import com.agendalo.domain.BusinessService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponseDto {
    private Long id;
    private String name;
    private String category;
    private String phone;
    private String address;
    private Integer minAdvanceDays;
    private Integer maxAdvanceDays;
    private Boolean hasTimeBetweenTurns;
    private Integer minutesBetweenTurns;
    private Integer cancellationHours;
    private List<BusinessHoursDto> businessHours;
    private List<BusinessServiceDto> services;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessHoursDto {
        private Long id;
        private String dayOfWeek;
        private String openingTime;
        private String closingTime;
        private boolean isWorkingDay;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessServiceDto {
        private Long id;
        private String name;
        private String description;
        private Integer durationMinutes;
        private Double price;
    }

    public static CompanyResponseDto fromEntity(Company company) {
        List<BusinessHoursDto> businessHoursDtos = company.getBusinessHours().stream()
                .map(hours -> BusinessHoursDto.builder()
                        .id(hours.getId())
                        .dayOfWeek(hours.getDayOfWeek().toString())
                        .openingTime(hours.getOpeningTime().toString())
                        .closingTime(hours.getClosingTime().toString())
                        .isWorkingDay(hours.isWorkingDay())
                        .build())
                .collect(Collectors.toList());

        List<BusinessServiceDto> serviceDtos = company.getServices().stream()
                .map(service -> BusinessServiceDto.builder()
                        .id(service.getId())
                        .name(service.getName())
                        .description(service.getDescription())
                        .durationMinutes(service.getDurationMinutes())
                        .price(service.getPrice())
                        .build())
                .collect(Collectors.toList());

        return CompanyResponseDto.builder()
                .id(company.getId())
                .name(company.getName())
                .category(company.getCategory())
                .phone(company.getPhone())
                .address(company.getAddress())
                .minAdvanceDays(company.getMinAdvanceDays())
                .maxAdvanceDays(company.getMaxAdvanceDays())
                .hasTimeBetweenTurns(company.getHasTimeBetweenTurns())
                .minutesBetweenTurns(company.getMinutesBetweenTurns())
                .cancellationHours(company.getCancellationHours())
                .businessHours(businessHoursDtos)
                .services(serviceDtos)
                .build();
    }
} 