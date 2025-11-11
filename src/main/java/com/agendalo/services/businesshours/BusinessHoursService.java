package com.agendalo.services.businesshours;

import com.agendalo.domain.BusinessHours;
import com.agendalo.domain.Company;
import com.agendalo.domain.Provider;
import com.agendalo.domain.User;
import com.agendalo.dto.businesshours.BusinessHoursRequestDto;
import com.agendalo.dto.businesshours.BusinessHoursSetRequestDto;
import com.agendalo.dto.response.ApiResponse;
import com.agendalo.repository.BusinessHoursRepository;
import com.agendalo.repository.ProviderRepository;
import com.agendalo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessHoursService {

    private final BusinessHoursRepository businessHoursRepository;
    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;

    @Transactional
    public ResponseEntity<ApiResponse> setBusinessHours(BusinessHoursSetRequestDto requestDto, String providerEmail) {
        log.info("Proveedor {} solicitando establecer horarios comerciales", providerEmail);

        // 1. Verificar que el usuario existe y es un proveedor
        Optional<User> userOptional = userRepository.findByEmail(providerEmail);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Usuario no encontrado", null));
        }

        // 2. Obtener el proveedor y verificar que tiene una empresa asociada
        Optional<Provider> providerOptional = providerRepository.findByUserId(userOptional.get().getId());
        if (providerOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "El usuario no es un proveedor", null));
        }

        Provider provider = providerOptional.get();
        Company company = provider.getCompany();

        if (company == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "El proveedor no tiene una empresa asociada", null));
        }

        try {
            // 3. Eliminar horarios existentes
            //businessHoursRepository.deleteByCompanyId(company.getId());

            // 4. Crear y guardar los nuevos horarios
            List<BusinessHours> savedBusinessHours = new ArrayList<>();
            
            for (BusinessHoursRequestDto hourDto : requestDto.getBusinessHours()) {
                // Validar el rango de horario
                if (!isValidTimeRange(hourDto)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ApiResponse(false, 
                                "Horario inválido para el día " + hourDto.getDay(), null));
                }

                BusinessHours businessHours = new BusinessHours();
                businessHours.setDayOfWeek(hourDto.getDay());
                businessHours.setOpeningTime(hourDto.getOpenTime());
                businessHours.setClosingTime(hourDto.getCloseTime());
                businessHours.setCompany(company);

                savedBusinessHours.add(businessHoursRepository.save(businessHours));
            }

            log.info("Horarios comerciales establecidos exitosamente para la empresa ID: {}", company.getId());
            return ResponseEntity.ok(new ApiResponse(true, 
                "Horarios comerciales establecidos exitosamente", savedBusinessHours));

        } catch (Exception e) {
            log.error("Error al establecer horarios comerciales: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, 
                        "Error al establecer horarios comerciales: " + e.getMessage(), null));
        }
    }

    public ResponseEntity<ApiResponse> getBusinessHours(String providerEmail) {
        log.info("Proveedor {} solicitando consultar horarios comerciales", providerEmail);

        // 1. Verificar que el usuario existe y es un proveedor
        Optional<User> userOptional = userRepository.findByEmail(providerEmail);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Usuario no encontrado", null));
        }

        // 2. Obtener el proveedor y verificar que tiene una empresa asociada
        Optional<Provider> providerOptional = providerRepository.findByUserId(userOptional.get().getId());
        if (providerOptional.isEmpty() || providerOptional.get().getCompany() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "El proveedor no tiene una empresa asociada", null));
        }

        Company company = providerOptional.get().getCompany();
        //List<BusinessHours> businessHours = businessHoursRepository.findByCompanyId(company.getId());

        return ResponseEntity.ok(new ApiResponse(true, 
            "Horarios comerciales recuperados exitosamente"));
    }

    private boolean isValidTimeRange(BusinessHoursRequestDto hourDto) {
        // Validar que la hora de apertura sea anterior a la de cierre
        if (hourDto.getOpenTime().isAfter(hourDto.getCloseTime())) {
            return false;
        }

        // Validar que los horarios no sean iguales
        if (hourDto.getOpenTime().equals(hourDto.getCloseTime())) {
            return false;
        }

        return true;
    }
} 