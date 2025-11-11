package com.agendalo.mapper;


import com.agendalo.domain.BusinessHours;
import com.agendalo.domain.BusinessService;
import com.agendalo.domain.Company;
import com.agendalo.dto.response.company.BusinessHoursResponse;
import com.agendalo.dto.response.company.CreateCompanyResponse;
import com.agendalo.dto.service.BusinessServiceResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CompanyMapper {

    public CreateCompanyResponse toDto(Company company) {
        if (company == null) {
            return null;
        }

        CreateCompanyResponse dto = new CreateCompanyResponse();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setCategory(company.getCategory());
        dto.setAddress(company.getAddress());
        dto.setPhone(company.getPhone());
        dto.setMinAdvanceDays(company.getMinAdvanceDays());
        dto.setMaxAdvanceDays(company.getMaxAdvanceDays());
        dto.setHasTimeBetweenTurns(company.getHasTimeBetweenTurns());
        dto.setMinutesBetweenTurns(company.getMinutesBetweenTurns());
        dto.setCancellationHours(company.getCancellationHours());

        // Mapear información del propietario
        if (company.getOwner() != null) {
            dto.setOwnerId(company.getOwner().getId());
            dto.setOwnerEmail(company.getOwner().getEmail());
        }

        // Mapear horarios de negocio
        if (company.getBusinessHours() != null) {
            List<BusinessHoursResponse> businessHoursDtos = company.getBusinessHours().stream()
                    .map(this::toBusinessHoursDto)
                    .collect(Collectors.toList());
            dto.setBusinessHours(businessHoursDtos);
        }

        // Mapear servicios
        if (company.getServices() != null) {
            List<BusinessServiceResponse> serviceDtos = company.getServices().stream()
                    .map(this::toBusinessServiceDto)
                    .collect(Collectors.toList());
            dto.setServices(serviceDtos);
        }

        return dto;
    }

    private BusinessHoursResponse toBusinessHoursDto(BusinessHours businessHours) {
        if (businessHours == null) {
            return null;
        }

        BusinessHoursResponse dto = new BusinessHoursResponse();
        dto.setId(businessHours.getId());
        dto.setDayOfWeek(businessHours.getDayOfWeek());
        dto.setOpeningTime(businessHours.getOpeningTime());
        dto.setClosingTime(businessHours.getClosingTime());
        dto.setWorkingDay(businessHours.isWorkingDay());

        return dto;
    }

    private BusinessServiceResponse toBusinessServiceDto(BusinessService service) {
        if (service == null) {
            return null;
        }

        BusinessServiceResponse dto = new BusinessServiceResponse();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setDescription(service.getDescription());
        dto.setDurationMinutes(service.getDurationMinutes());
        dto.setPrice(service.getPrice());

        return dto;
    }
}