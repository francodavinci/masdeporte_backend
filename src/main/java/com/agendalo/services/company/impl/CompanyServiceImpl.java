package com.agendalo.services.company.impl;

import com.agendalo.domain.*;
import com.agendalo.domain.enums.ImageType;
import com.agendalo.domain.enums.CompanyStatus;
import com.agendalo.dto.businesshours.BusinessHoursRequestDto;
import com.agendalo.dto.response.company.CreateCompanyResponse;
import com.agendalo.dto.company.CompanyRegistrationDto;
import com.agendalo.dto.company.CompanyStatusResponseDto;
import com.agendalo.dto.company.CompanyStatusUpdateDto;
import com.agendalo.dto.company.PendingCompanyDto;
import com.agendalo.dto.service.ServiceRegistrationDto;
import com.agendalo.dto.response.ApiResponse;
import com.agendalo.dto.service.ServiceUpdateDto;
import com.agendalo.mapper.CompanyMapper;
import com.agendalo.repository.*;
import com.agendalo.repository.BusinessHoursRepository;
import com.agendalo.repository.BusinessServiceRepository;
import com.agendalo.repository.CompanyRepository;
import com.agendalo.repository.UserRepository;
import com.agendalo.exceptions.ResourceNotFoundException;
import com.agendalo.services.company.CompanyService;
import com.agendalo.services.email.EmailService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final BusinessHoursRepository businessHoursRepository;
    private final BusinessServiceRepository businessServiceRepository;
    private final EmailService emailService;
    private final CompanyMapper companyMapper;


    public CompanyServiceImpl(CompanyRepository companyRepository,
                              UserRepository userRepository,
                              BusinessHoursRepository businessHoursRepository,
                              BusinessServiceRepository businessServiceRepository,
                              CompanyMapper companyMapper,
                              ImageRepository imageRepository,
                              EmailService emailService
    ) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.businessHoursRepository = businessHoursRepository;
        this.businessServiceRepository = businessServiceRepository;
        this.companyMapper = companyMapper;
        this.imageRepository = imageRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> create(CompanyRegistrationDto dto, String ownerEmail) {

        log.info("Creando compañía: {}", dto.getCompanyName());

        try {

            if (companyRepository.existsByUrlSlug(dto.getUrlSlug())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "Ya existe una empresa con esta URL. Por favor elige otra."));
            }

            // Buscar el usuario por email
            User owner = userRepository.findByEmail(ownerEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + ownerEmail));


            // Crear la entidad Company
            Company company = new Company();
            company.setOwner(owner);
            company.setName(dto.getCompanyName());
            company.setCategory(dto.getCategory());
            company.setPhone(dto.getPhone());
            company.setAddress(dto.getAddress());
            company.setLatitude(dto.getLatitude());
            company.setLongitude(dto.getLongitude());
            company.setUrlSlug(dto.getUrlSlug());
            company.setMinAdvanceDays(dto.getMinAdvanceDays());
            company.setMaxAdvanceDays(dto.getMaxAdvanceDays());
            company.setHasTimeBetweenTurns(dto.getHasTimeBetweenTurns());
            company.setMinutesBetweenTurns(dto.getMinutesBetweenTurns());
            company.setCancellationHours(dto.getCancellationHours());
            company.setStatus(CompanyStatus.PENDING);

            // Guardar la compañía para obtener su ID
            company = companyRepository.save(company);

            // Crear y guardar los horarios de negocio
            Set<BusinessHours> businessHoursSet = new HashSet<>();

            // Procesar los horarios enviados
            if (dto.getBusinessHours() != null) {
                for (BusinessHoursRequestDto hoursDto : dto.getBusinessHours()) {
                    BusinessHours hours = new BusinessHours();
                    hours.setCompany(company);
                    hours.setDayOfWeek(hoursDto.getDay()); // Cambio de getDayOfWeek() a getDay()
                    hours.setOpeningTime(hoursDto.getOpenTime()); // Cambio de getOpeningTime() a getOpenTime()
                    hours.setClosingTime(hoursDto.getCloseTime()); // Cambio de getClosingTime() a getCloseTime()
                    hours.setWorkingDay(true);
                    businessHoursSet.add(hours);
                }
            }

            // Añadir los días no configurados como no laborables
            Set<DayOfWeek> configuredDays = businessHoursSet.stream()
                    .map(BusinessHours::getDayOfWeek)
                    .collect(Collectors.toSet());

            for (DayOfWeek day : DayOfWeek.values()) {
                if (!configuredDays.contains(day)) {
                    BusinessHours hours = new BusinessHours();
                    hours.setCompany(company);
                    hours.setDayOfWeek(day);
                    hours.setOpeningTime(LocalTime.of(0, 0));
                    hours.setClosingTime(LocalTime.of(0, 0));
                    hours.setWorkingDay(false);
                    businessHoursSet.add(hours);
                }
            }

            // Guardar todos los horarios
            businessHoursRepository.saveAll(businessHoursSet);

            // Convertir la entidad a DTO para la respuesta
            CreateCompanyResponse responseDto = companyMapper.toDto(company);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Compañía creada exitosamente con ID: " + company.getId(), company));

        } catch (Exception e) {
            log.error("Error al crear la compañía", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al crear la compañía: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> update(CompanyRegistrationDto dto, String ownerEmail) {

        log.info("Actualizando compañía: {}", dto.getCompanyName());

        try {

            // Buscar el usuario por email
            User owner = userRepository.findByEmail(ownerEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + ownerEmail));

            Company companyFound = companyRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Compañía no encontrada con id: " + dto.getId()));
            // Crear la entidad Company
            Company company = new Company();
            company.setId(dto.getId());
            company.setOwner(owner);
            company.setName(dto.getCompanyName());
            company.setCategory(dto.getCategory());
            company.setPhone(dto.getPhone());
            company.setAddress(dto.getAddress());
            company.setLatitude(dto.getLatitude());
            company.setLongitude(dto.getLongitude());
            company.setUrlSlug(dto.getUrlSlug());
            company.setMinAdvanceDays(dto.getMinAdvanceDays());
            company.setMaxAdvanceDays(dto.getMaxAdvanceDays());
            company.setHasTimeBetweenTurns(dto.getHasTimeBetweenTurns());
            company.setMinutesBetweenTurns(dto.getMinutesBetweenTurns());
            company.setCancellationHours(dto.getCancellationHours());
            company.setStatus(companyFound.getStatus());
            company.setRegistrationDate(companyFound.getRegistrationDate());
            // Guardar la compañía para obtener su ID
            company = companyRepository.saveAndFlush(company);

            // Crear y guardar los horarios de negocio
            Set<BusinessHours> businessHoursSet = new HashSet<>();

            // Procesar los horarios enviados
            if (dto.getBusinessHours() != null) {
                for (BusinessHoursRequestDto hoursDto : dto.getBusinessHours()) {
                    BusinessHours hours = new BusinessHours();
                    hours.setCompany(company);
                    hours.setDayOfWeek(hoursDto.getDay()); // Cambio de getDayOfWeek() a getDay()
                    hours.setOpeningTime(hoursDto.getOpenTime()); // Cambio de getOpeningTime() a getOpenTime()
                    hours.setClosingTime(hoursDto.getCloseTime()); // Cambio de getClosingTime() a getCloseTime()
                    hours.setWorkingDay(true);
                    businessHoursSet.add(hours);
                }
            }

            // Añadir los días no configurados como no laborables
            Set<DayOfWeek> configuredDays = businessHoursSet.stream()
                    .map(BusinessHours::getDayOfWeek)
                    .collect(Collectors.toSet());

            for (DayOfWeek day : DayOfWeek.values()) {
                if (!configuredDays.contains(day)) {
                    BusinessHours hours = new BusinessHours();
                    hours.setCompany(company);
                    hours.setDayOfWeek(day);
                    hours.setOpeningTime(LocalTime.of(0, 0));
                    hours.setClosingTime(LocalTime.of(0, 0));
                    hours.setWorkingDay(false);
                    businessHoursSet.add(hours);
                }
            }

            // Guardar todos los horarios
            businessHoursRepository.saveAll(businessHoursSet);

            // Convertir la entidad a DTO para la respuesta
            CreateCompanyResponse responseDto = companyMapper.toDto(company);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Compañía actualizada exitosamente con ID: " + company.getId(), company));

        } catch (Exception e) {
            log.error("Error al crear la compañía", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al crear la compañía: " + e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse> createService(ServiceRegistrationDto serviceRegistrationDto, String ownerEmail) {
        log.info("Usuario {} solicitando crear servicio", ownerEmail);

        try {
            // 1. Verificar que el usuario existe
            User owner = userRepository.findByEmail(ownerEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + ownerEmail));

            // 2. Obtener la compañía del usuario
            List<Company> userCompanies = companyRepository.findByOwnerId(owner.getId());
            if (userCompanies.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "El usuario no tiene una compañía asociada"));
            }

            Company company = userCompanies.get(0);

            // 3. Verificar que no exceda el límite de servicios
            if (company.getServices().size() >= 3) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "Una compañía no puede tener más de 3 servicios"));
            }


            // 4. Crear el servicio
            BusinessService service = new BusinessService();
            service.setName(serviceRegistrationDto.getName());
            service.setDescription(serviceRegistrationDto.getDescription());
            service.setDurationMinutes(serviceRegistrationDto.getDurationMinutes());
            service.setPrice(serviceRegistrationDto.getPrice());
            service.setCompany(company);
            company.addService(service);
            businessServiceRepository.save(service);


            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Servicio creado exitosamente", company.getServices()));

        } catch (Exception e) {
            log.error("Error al crear el servicio", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al crear el servicio: " + e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse> updateService(Long serviceId, ServiceUpdateDto serviceUpdateDto, String ownerEmail) {
        log.info("Usuario {} solicitando actualizar servicio con ID: {}", ownerEmail, serviceId);

        try {
            // 1. Verificar que el usuario existe
            User owner = userRepository.findByEmail(ownerEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + ownerEmail));

            // 2. Obtener la compañía del usuario
            List<Company> userCompanies = companyRepository.findByOwnerId(owner.getId());
            if (userCompanies.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "El usuario no tiene una compañía asociada"));
            }

            Company company = userCompanies.get(0);

            // 3. Buscar el servicio
            Optional<BusinessService> serviceOptional = businessServiceRepository.findById(serviceId);
            if (serviceOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse(false, "Servicio no encontrado"));
            }

            BusinessService service = serviceOptional.get();

            // 4. Verificar que el servicio pertenece a la compañía del usuario
            if (!service.getCompany().getId().equals(company.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "No tienes permiso para modificar este servicio"));
            }

            // 5. Actualizar el servicio
            service.setName(serviceUpdateDto.getName());
            service.setDescription(serviceUpdateDto.getDescription());
            service.setDurationMinutes(serviceUpdateDto.getDurationMinutes());
            service.setPrice(serviceUpdateDto.getPrice());

            // 6. Guardar los cambios
            BusinessService updatedService = businessServiceRepository.save(service);

            return ResponseEntity.ok(new ApiResponse(true, "Servicio actualizado exitosamente", updatedService));

        } catch (Exception e) {
            log.error("Error al actualizar el servicio", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al actualizar el servicio: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getCompanyById(Long id) {
        log.info("Obteniendo empresa por ID: {}", id);

        try {
            Optional<Company> companyOptional = companyRepository.findById(id);

            if (companyOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse(false, "Empresa no encontrada"));
            }

            Company company = companyOptional.get();

            // Convertir la entidad a DTO para la respuesta
            CompanyRegistrationDto companyDto = new CompanyRegistrationDto();
            companyDto.setId(company.getId());
            companyDto.setCompanyName(company.getName());
            companyDto.setCategory(company.getCategory());
            companyDto.setPhone(company.getPhone());
            companyDto.setAddress(company.getAddress());
            companyDto.setUrlSlug(company.getUrlSlug());
            companyDto.setMinAdvanceDays(company.getMinAdvanceDays());
            companyDto.setMaxAdvanceDays(company.getMaxAdvanceDays());
            companyDto.setHasTimeBetweenTurns(company.getHasTimeBetweenTurns());
            companyDto.setMinutesBetweenTurns(company.getMinutesBetweenTurns());
            companyDto.setCancellationHours(company.getCancellationHours());
            companyDto.setLatitude(company.getLatitude());
            companyDto.setLongitude(company.getLongitude());
            companyDto.setOwnerEmail(company.getOwner().getEmail());
            companyDto.setOwnerName(company.getOwner().getName());
            companyDto.setRegistrationDate(company.getRegistrationDate());
            companyDto.setRequestStatus(company.getStatus());
            // Buscar el logo de la compañía
            Image logo = imageRepository.findByCompanyIdAndImageType(company.getId(), ImageType.LOGO);

            log.info("logo {}", logo);
            if (logo != null) {
                companyDto.setLogoId(logo.getId());
                companyDto.setLogoUrl("/api/images/" + logo.getId());
                companyDto.setLogoData(logo.getData());
            }

            // Mapear los horarios de negocio
            if (company.getBusinessHours() != null) {
                List<BusinessHoursRequestDto> businessHoursDtos = company.getBusinessHours().stream()
                        .filter(hours -> hours.isWorkingDay())
                        .map(hours -> {
                            BusinessHoursRequestDto hoursDto = new BusinessHoursRequestDto();
                            hoursDto.setDay(hours.getDayOfWeek());
                            hoursDto.setOpenTime(hours.getOpeningTime());
                            hoursDto.setCloseTime(hours.getClosingTime());
                            return hoursDto;
                        })
                        .collect(Collectors.toList());
                companyDto.setBusinessHours(businessHoursDtos);
            }

            return ResponseEntity.ok(new ApiResponse(true, "Empresa obtenida exitosamente", companyDto));

        } catch (Exception e) {
            log.error("Error al obtener la empresa por ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al obtener la empresa: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> findAll() {
        log.info("Obteniendo todas las compañías");

        try {
            // Obtener todas las compañías de la base de datos
            List<Company> companies = companyRepository.findByStatus(CompanyStatus.ACCEPTED);

            if (companies.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse(true, "No hay compañías registradas", new ArrayList<>()));
            }

            // Convertir las entidades a DTOs para la respuesta
            List<CompanyRegistrationDto> companyDtos = companies.stream()
                    .map(company -> {
                        CompanyRegistrationDto dto = new CompanyRegistrationDto();
                        dto.setCompanyName(company.getName());
                        dto.setCategory(company.getCategory());
                        dto.setPhone(company.getPhone());
                        dto.setAddress(company.getAddress());
                        dto.setUrlSlug(company.getUrlSlug());
                        dto.setMinAdvanceDays(company.getMinAdvanceDays());
                        dto.setMaxAdvanceDays(company.getMaxAdvanceDays());
                        dto.setHasTimeBetweenTurns(company.getHasTimeBetweenTurns());
                        dto.setMinutesBetweenTurns(company.getMinutesBetweenTurns());
                        dto.setCancellationHours(company.getCancellationHours());
                        dto.setId(company.getId());
                        dto.setLatitude(company.getLatitude());
                        dto.setLongitude(company.getLongitude());
                        // Buscar el logo de la compañía
                        Image logo = imageRepository.findByCompanyIdAndImageType(company.getId(), ImageType.LOGO);
                        if (logo != null) {
                            dto.setLogoId(logo.getId());
                            // Construir la URL para acceder al logo
                            dto.setLogoUrl("/api/images/" + logo.getId());
                        }

                        // Mapear los horarios de negocio
                        if (company.getBusinessHours() != null) {
                            List<BusinessHoursRequestDto> businessHoursDtos = company.getBusinessHours().stream()
                                    .filter(hours -> hours.isWorkingDay())
                                    .map(hours -> {
                                        BusinessHoursRequestDto hoursDto = new BusinessHoursRequestDto();
                                        hoursDto.setDay(hours.getDayOfWeek());
                                        hoursDto.setOpenTime(hours.getOpeningTime());
                                        hoursDto.setCloseTime(hours.getClosingTime());
                                        return hoursDto;
                                    })
                                    .collect(Collectors.toList());
                            dto.setBusinessHours(businessHoursDtos);
                        }

                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(true, "Compañías obtenidas exitosamente", companyDtos));

        } catch (Exception e) {
            log.error("Error al obtener todas las compañías", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al obtener las compañías: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> searchCompanies(String query, String location) {
        log.info("Buscando compañías con query: '{}' y location: '{}'", query, location);

        try {
            List<Company> companies;

            if (query != null && !query.trim().isEmpty() && location != null && !location.trim().isEmpty()) {
                // Buscar por nombre Y ubicación - solo empresas aceptadas
                companies = companyRepository.findByNameContainingIgnoreCaseAndAddressContainingIgnoreCaseAndStatus(
                        query.trim(), location.trim(), CompanyStatus.ACCEPTED);
            } else if (query != null && !query.trim().isEmpty()) {
                // Buscar solo por nombre - solo empresas aceptadas
                companies = companyRepository.findByNameContainingIgnoreCaseAndStatus(query.trim(), CompanyStatus.ACCEPTED);
            } else if (location != null && !location.trim().isEmpty()) {
                // Buscar solo por ubicación - solo empresas aceptadas
                companies = companyRepository.findByAddressContainingIgnoreCaseAndStatus(location.trim(), CompanyStatus.ACCEPTED);
            } else {
                // Si no hay parámetros, devolver todas las compañías aceptadas
                companies = companyRepository.findByStatus(CompanyStatus.ACCEPTED);
            }

            if (companies.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse(true,
                        "No se encontraron compañías aceptadas con los criterios especificados",
                        new ArrayList<>()));
            }


            // Convertir las entidades a DTOs (igual que en findAll)
            List<CompanyRegistrationDto> companyDtos = companies.stream()
                    .map(company -> {
                        CompanyRegistrationDto dto = new CompanyRegistrationDto();
                        dto.setCompanyName(company.getName());
                        dto.setCategory(company.getCategory());
                        dto.setPhone(company.getPhone());
                        dto.setAddress(company.getAddress());
                        dto.setUrlSlug(company.getUrlSlug());
                        dto.setMinAdvanceDays(company.getMinAdvanceDays());
                        dto.setMaxAdvanceDays(company.getMaxAdvanceDays());
                        dto.setHasTimeBetweenTurns(company.getHasTimeBetweenTurns());
                        dto.setMinutesBetweenTurns(company.getMinutesBetweenTurns());
                        dto.setCancellationHours(company.getCancellationHours());
                        dto.setId(company.getId());

                        // Buscar el logo de la compañía
                        Image logo = imageRepository.findByCompanyIdAndImageType(company.getId(), ImageType.LOGO);
                        if (logo != null) {
                            dto.setLogoId(logo.getId());
                            dto.setLogoUrl("/api/images/" + logo.getId());
                        }

                        // Mapear los horarios de negocio
                        if (company.getBusinessHours() != null) {
                            List<BusinessHoursRequestDto> businessHoursDtos = company.getBusinessHours().stream()
                                    .filter(hours -> hours.isWorkingDay())
                                    .map(hours -> {
                                        BusinessHoursRequestDto hoursDto = new BusinessHoursRequestDto();
                                        hoursDto.setDay(hours.getDayOfWeek());
                                        hoursDto.setOpenTime(hours.getOpeningTime());
                                        hoursDto.setCloseTime(hours.getClosingTime());
                                        return hoursDto;
                                    })
                                    .collect(Collectors.toList());
                            dto.setBusinessHours(businessHoursDtos);
                        }

                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(true,
                    "Compañías encontradas exitosamente", companyDtos));

        } catch (Exception e) {
            log.error("Error al buscar compañías", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false,
                            "Error al buscar las compañías: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getCompanyBySlug(String urlSlug) {
        log.info("Buscando compañía con slug: {}", urlSlug);

        try {
            Optional<Company> companyOptional = companyRepository.findByUrlSlug(urlSlug);

            if (companyOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse(false, "Compañía no encontrada"));
            }

            Company company = companyOptional.get();

            // Verificar que la compañía esté aceptada
            if (company.getStatus() != CompanyStatus.ACCEPTED) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse(false, "Compañía no disponible"));
            }

            return ResponseEntity.ok(new ApiResponse(true, "Compañía encontrada", company));

        } catch (Exception e) {
            log.error("Error al buscar la compañía por slug: {}", urlSlug, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al buscar la compañía: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> findUrlSlug(String urlSlug) {
        Optional<Company> companyFound = companyRepository.findByUrlSlug(urlSlug);

        boolean companyFoundPresent = companyFound.isPresent();

        return ResponseEntity.ok(new ApiResponse(companyFoundPresent, "Busqueda de urlSlug realizada correctamente"));

    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> deleteService(Long serviceId, String ownerEmail) {
        log.info("Usuario {} solicitando eliminar servicio con ID: {}", ownerEmail, serviceId);

        try {
            // 1. Verificar que el usuario existe
            User owner = userRepository.findByEmail(ownerEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + ownerEmail));

            // 2. Obtener la compañía del usuario
            List<Company> userCompanies = companyRepository.findByOwnerId(owner.getId());
            if (userCompanies.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "El usuario no tiene una compañía asociada"));
            }

            Company company = userCompanies.get(0);

            // 3. Buscar el servicio
            Optional<BusinessService> serviceOptional = businessServiceRepository.findById(serviceId);
            if (serviceOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse(false, "Servicio no encontrado"));
            }

            BusinessService service = serviceOptional.get();

            // 4. Verificar que el servicio pertenece a la compañía del usuario
            if (!service.getCompany().getId().equals(company.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "No tienes permiso para eliminar este servicio"));
            }

            // 5. Eliminar el servicio
            businessServiceRepository.delete(service);

            return ResponseEntity.ok(new ApiResponse(true, "Servicio eliminado exitosamente"));

        } catch (Exception e) {
            log.error("Error al eliminar el servicio", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al eliminar el servicio: " + e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse> getServices(String ownerEmail) {
        log.info("Usuario {} solicitando obtener servicios", ownerEmail);

        try {
            // 1. Verificar que el usuario existe
            User owner = userRepository.findByEmail(ownerEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + ownerEmail));

            // 2. Obtener la compañía del usuario
            List<Company> userCompanies = companyRepository.findByOwnerId(owner.getId());
            if (userCompanies.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "El usuario no tiene una compañía asociada"));
            }

            Company company = userCompanies.get(0);

            // 3. Obtener los servicios de la compañía
            List<BusinessService> services = businessServiceRepository.findByCompanyId(company.getId());

            return ResponseEntity.ok(new ApiResponse(true, "Servicios obtenidos exitosamente", services));

        } catch (Exception e) {
            log.error("Error al obtener los servicios", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al obtener los servicios: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getCompanyByOwnerEmail(String ownerEmail) {
        log.info("Buscando compañía del usuario: {}", ownerEmail);

        try {
            Optional<Company> companyOptional = companyRepository.findByOwner_Email(ownerEmail);

            if (companyOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse(false, "No se encontró una compañía para el usuario: " + ownerEmail));
            }

            Company company = companyOptional.get();

            // Convertir la entidad a DTO para la respuesta
            CompanyRegistrationDto companyDto = new CompanyRegistrationDto();
            companyDto.setCompanyName(company.getName());
            companyDto.setCategory(company.getCategory());
            companyDto.setPhone(company.getPhone());
            companyDto.setAddress(company.getAddress());
            companyDto.setUrlSlug(company.getUrlSlug());
            companyDto.setMinAdvanceDays(company.getMinAdvanceDays());
            companyDto.setMaxAdvanceDays(company.getMaxAdvanceDays());
            companyDto.setHasTimeBetweenTurns(company.getHasTimeBetweenTurns());
            companyDto.setMinutesBetweenTurns(company.getMinutesBetweenTurns());
            companyDto.setCancellationHours(company.getCancellationHours());
            companyDto.setId(company.getId());
            companyDto.setLatitude(company.getLatitude());
            companyDto.setLongitude(company.getLongitude());
            companyDto.setRequestStatus(company.getStatus());
            // Buscar el logo de la compañía
            Image logo = imageRepository.findByCompanyIdAndImageType(company.getId(), ImageType.LOGO);
            if (logo != null) {
                companyDto.setLogoId(logo.getId());
                companyDto.setLogoUrl("/api/images/" + logo.getId());
            }

            // Mapear los horarios de negocio
            if (company.getBusinessHours() != null) {
                List<BusinessHoursRequestDto> businessHoursDtos = company.getBusinessHours().stream()
                        .filter(hours -> hours.isWorkingDay())
                        .map(hours -> {
                            BusinessHoursRequestDto hoursDto = new BusinessHoursRequestDto();
                            hoursDto.setDay(hours.getDayOfWeek());
                            hoursDto.setOpenTime(hours.getOpeningTime());
                            hoursDto.setCloseTime(hours.getClosingTime());
                            return hoursDto;
                        })
                        .collect(Collectors.toList());
                companyDto.setBusinessHours(businessHoursDtos);
            }

            return ResponseEntity.ok(new ApiResponse(true, "Compañía encontrada exitosamente", companyDto));

        } catch (Exception e) {
            log.error("Error al buscar la compañía del usuario: {}", ownerEmail, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al buscar la compañía: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getPendingCompanies() {
        try {
            log.info("Obteniendo compañías con status PENDING");

            List<Company> pendingCompaniesData = companyRepository.findAll();


            if (pendingCompaniesData.isEmpty()) {
                log.info("No se encontraron compañías pendientes");
                return ResponseEntity.ok(new ApiResponse(true, "No hay compañías pendientes", List.of()));
            }

            List<PendingCompanyDto> pendingCompanyDtos = pendingCompaniesData.stream()
                    .map(company -> {
                        // Obtener la imagen del logo si existe
                        String base64Image = null;
                        if (company.getGalleryImages() != null && !company.getGalleryImages().isEmpty()) {
                            Image logoImage = company.getGalleryImages().stream()
                                    .filter(img -> img.getImageType().toString().equals("LOGO"))
                                    .findFirst()
                                    .orElse(null);

                            if (logoImage != null && logoImage.getData() != null) {
                                base64Image = java.util.Base64.getEncoder().encodeToString(logoImage.getData());
                            }
                        }

                        return PendingCompanyDto.builder()
                                .id(company.getId())
                                .companyName(company.getName())
                                .ownerName(company.getOwner().getEmail())
                                .status(company.getStatus().toString())
                                .registrationDate(company.getRegistrationDate())
                                .companyImage(base64Image)
                                .build();
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(true, "Compañías pendientes obtenidas exitosamente", pendingCompanyDtos));

        } catch (Exception e) {
            log.error("Error al obtener las compañías pendientes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al obtener las compañías pendientes: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> updateCompanyStatus(Long companyId, CompanyStatusUpdateDto statusUpdateDto, String adminEmail) {
        log.info("Iniciando cambio de status de compañía ID: {} a status: {} por admin: {}",
                companyId, statusUpdateDto.getStatus(), adminEmail);

        try {
            // Verificar que el usuario existe y tiene rol ADMIN
            User admin = userRepository.findByEmail(adminEmail)
                    .orElseThrow(() -> {
                        log.warn("Intento de cambio de status por usuario no existente: {}", adminEmail);
                        return new ResourceNotFoundException("Usuario no encontrado");
                    });

            if (!"ADMIN".equals(admin.getRole())) {
                log.warn("Intento de cambio de status por usuario sin rol ADMIN: {} (rol: {})",
                        adminEmail, admin.getRole());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "Solo los administradores pueden cambiar el status de las compañías"));
            }

            // Verificar que la compañía existe
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> {
                        log.warn("Intento de cambio de status de compañía no existente ID: {}", companyId);
                        return new ResourceNotFoundException("Compañía no encontrada");
                    });

            // Guardar el status anterior para el log
            CompanyStatus previousStatus = company.getStatus();
            CompanyStatus newStatus = statusUpdateDto.getStatus();

            if (previousStatus == newStatus) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "El status actual de la compania es igual al status que se quiere actualizar"));
            }

            // Actualizar el status

            if (statusUpdateDto.getStatus().equals(CompanyStatus.CANCELLED)) {
                // mandar rejection email (asíncrono)
                emailService.sendCompanyCancellationEmail(company, statusUpdateDto.getReason())
                    .exceptionally(throwable -> {
                        log.error("Error asíncrono al enviar email de cancelación de empresa ID: {}. Error: {}",
                                 company.getId(), throwable.getMessage(), throwable);
                        return null;
                    });
            }

            if (previousStatus.equals(CompanyStatus.PENDING) && newStatus.equals(CompanyStatus.ACCEPTED)) {
                emailService.sendCompanyApproveEmail(company)
                    .exceptionally(throwable -> {
                        log.error("Error asíncrono al enviar email de aprobación de empresa ID: {}. Error: {}",
                                 company.getId(), throwable.getMessage(), throwable);
                        return null;
                    });
                // Por ultimo se cambia el ROLE del usuario a OWNER para poder diferenciarlo
                User owner = company.getOwner();
                owner.setRole("OWNER");
                userRepository.save(owner);
            }

            company.setStatus(newStatus);
            Company updatedCompany = companyRepository.save(company);

            log.info("Status de compañía actualizado exitosamente - ID: {}, Status anterior: {}, Nuevo status: {}, Admin: {}",
                    companyId, previousStatus, updatedCompany.getStatus(), adminEmail);

            // Crear DTO de respuesta para evitar problemas de lazy loading
            CompanyStatusResponseDto responseDto = CompanyStatusResponseDto.builder()
                    .id(updatedCompany.getId())
                    .name(updatedCompany.getName())
                    .category(updatedCompany.getCategory())
                    .phone(updatedCompany.getPhone())
                    .address(updatedCompany.getAddress())
                    .status(updatedCompany.getStatus())
                    .registrationDate(updatedCompany.getRegistrationDate())
                    .ownerEmail(updatedCompany.getOwner().getEmail())
                    .build();

            return ResponseEntity.ok(new ApiResponse(true,
                    String.format("Status de la compañía '%s' actualizado de %s a %s exitosamente",
                            updatedCompany.getName(), previousStatus, updatedCompany.getStatus()),
                    responseDto));

        } catch (ResourceNotFoundException e) {
            log.error("Error al cambiar status de compañía: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Error inesperado al cambiar status de compañía ID: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error interno al cambiar el status de la compañía: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> ownerUpdateStatus(Long companyId, CompanyStatusUpdateDto statusUpdateDto, String adminEmail) {
        log.info("Iniciando cambio de status de compañía ID: {} a status: {} por owner: {}",
                companyId, statusUpdateDto.getStatus(), adminEmail);

        try {
            // Verificar que el usuario existe y tiene rol ADMIN
            User admin = userRepository.findByEmail(adminEmail)
                    .orElseThrow(() -> {
                        log.warn("Intento de cambio de status por usuario no existente: {}", adminEmail);
                        return new ResourceNotFoundException("Usuario no encontrado");
                    });

            // Verificar que la compañía existe
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> {
                        log.warn("Intento de cambio de status de compañía no existente ID: {}", companyId);
                        return new ResourceNotFoundException("Compañía no encontrada");
                    });

            // Guardar el status anterior para el log
            CompanyStatus previousStatus = company.getStatus();
            CompanyStatus newStatus = statusUpdateDto.getStatus();

            if (previousStatus == newStatus) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "El status actual de la compania es igual al status que se quiere actualizar"));
            }

            company.setStatus(newStatus);
            Company updatedCompany = companyRepository.save(company);

            log.info("Status de compañía actualizado exitosamente - ID: {}, Status anterior: {}, Nuevo status: {}, Owner: {}",
                    companyId, previousStatus, updatedCompany.getStatus(), adminEmail);

            // Crear DTO de respuesta para evitar problemas de lazy loading
            CompanyStatusResponseDto responseDto = CompanyStatusResponseDto.builder()
                    .id(updatedCompany.getId())
                    .name(updatedCompany.getName())
                    .category(updatedCompany.getCategory())
                    .phone(updatedCompany.getPhone())
                    .address(updatedCompany.getAddress())
                    .status(updatedCompany.getStatus())
                    .registrationDate(updatedCompany.getRegistrationDate())
                    .ownerEmail(updatedCompany.getOwner().getEmail())
                    .build();

            return ResponseEntity.ok(new ApiResponse(true,
                    String.format("Status de la compañía '%s' actualizado de %s a %s exitosamente",
                            updatedCompany.getName(), previousStatus, updatedCompany.getStatus()),
                    responseDto));

        } catch (ResourceNotFoundException e) {
            log.error("Error al cambiar status de compañía: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Error inesperado al cambiar status de compañía ID: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error interno al cambiar el status de la compañía: " + e.getMessage()));
        }
    }

}