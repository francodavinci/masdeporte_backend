package com.agendalo.services.appointment.impl;

import com.agendalo.domain.*;
import com.agendalo.dto.appointment.AppointmentDto;
import com.agendalo.dto.appointment.CreateAppointmentRequest;
import com.agendalo.dto.appointment.UpdateAppointmentStatusRequest;
import com.agendalo.dto.coupon.CouponUsageDto;
import com.agendalo.dto.response.ApiResponse;
import com.agendalo.dto.response.availability.AvailabilityResponse;
import com.agendalo.domain.enums.AppointmentStatus;
import com.agendalo.repository.*;
import com.agendalo.services.appointment.AppointmentService;
import com.agendalo.services.calendar.GoogleCalendarService;
import com.agendalo.services.coupon.CouponService;
import com.agendalo.services.mercado_pago.MercadoPagoService;
import com.agendalo.services.email.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final BusinessServiceRepository businessServiceRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final BusinessHoursRepository businessHoursRepository;

    private final MercadoPagoService mercadoPagoService;
    private final EmailService emailService;
    private final GoogleCalendarService googleCalendarService;

    public AppointmentServiceImpl(
            AppointmentRepository appointmentRepository,
            BusinessServiceRepository businessServiceRepository,
            UserRepository userRepository,
            CompanyRepository companyRepository,
            BusinessHoursRepository businessHoursRepository,
            MercadoPagoService mercadoPagoService,
            GoogleCalendarService googleCalendarService,
            CouponService couponService,
            EmailService emailService) {
        this.appointmentRepository = appointmentRepository;
        this.businessServiceRepository = businessServiceRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.businessHoursRepository = businessHoursRepository;
        this.mercadoPagoService = mercadoPagoService;
        this.emailService = emailService;
        this.googleCalendarService = googleCalendarService;
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> createAppointment(CreateAppointmentRequest request, String userEmail) {
        log.info("Usuario {} solicitando crear turno", userEmail);

        try {
            // 1. Verificar que el usuario existe
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

            // 2. Obtener el servicio
            BusinessService service = businessServiceRepository.findById(request.getServiceId())
                    .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

            // 3. Obtener la compañía asociada al servicio
            Company company = service.getCompany();

            // 4. Calcular la hora de finalización basada en la duración del servicio
            LocalDateTime startTime = request.getStartTime();
            LocalDateTime endTime = startTime.plusMinutes(service.getDurationMinutes());

            // 5. Validaciones
            validateAppointmentRequest(company, service, startTime, endTime);

            // 6. Crear y guardar el turno
            Appointment appointment = new Appointment();
            appointment.setService(service);
            appointment.setUser(user);
            appointment.setCompany(company);
            appointment.setStartTime(startTime);
            appointment.setEndTime(endTime);
            appointment.setStatus(AppointmentStatus.PENDING);
            appointment.setNotes(request.getNotes());

            Appointment savedAppointment = appointmentRepository.save(appointment);

            // NUEVA INTEGRACIÓN: Crear evento en Google Calendar
            try {
                if (googleCalendarService.isConfigured(savedAppointment.getCompany().getId())) {
                    googleCalendarService.createAppointmentEvent(savedAppointment);
                    log.info("Evento creado en Google Calendar para turno: {}", savedAppointment.getId());
                } else {
                    log.info("Google Calendar no configurado para la compañía: {}", savedAppointment.getCompany().getId());
                }
            } catch (Exception e) {
                log.error("Error creando evento en Google Calendar para turno: {}", savedAppointment.getId(), e);
                // No fallar la creación del turno si falla el calendario
            }

            // ... resto de tu lógica existente ...
            // 7. Enviar emails de notificación (asíncrono - no bloquea el flujo principal)
            try {
                emailService.sendAppointmentConfirmationToUser(savedAppointment)
                    .exceptionally(throwable -> {
                        log.error("Error asíncrono al enviar email de confirmación al usuario para turno ID: {}. Error: {}",
                                 savedAppointment.getId(), throwable.getMessage(), throwable);
                        return null;
                    });
                
                emailService.sendAppointmentNotificationToCompany(savedAppointment)
                    .exceptionally(throwable -> {
                        log.error("Error asíncrono al enviar email de notificación a la empresa para turno ID: {}. Error: {}",
                                 savedAppointment.getId(), throwable.getMessage(), throwable);
                        return null;
                    });
                
                log.info("Emails de notificación programados para envío asíncrono para el turno ID: {}", savedAppointment.getId());
            } catch (Exception e) {
                log.error("Error al programar envío de emails para el turno ID: {}. Error: {}",
                         savedAppointment.getId(), e.getMessage(), e);
                // No fallar la creación del turno si falla el envío de email
            }

            // 8. Retornar respuesta
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Turno creado exitosamente", mapToDTO(savedAppointment)));

        } catch (Exception e) {
            log.error("Error al crear el turno", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al crear el turno: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getAppointmentById(Long id, String userEmail) {
        log.info("Usuario {} solicitando obtener turno con ID: {}", userEmail, id);

        try {
            // 1. Verificar que el usuario existe
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

            // 2. Buscar el turno
            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

            // 3. Verificar que el usuario tiene permiso para ver este turno
            // (es el usuario del turno o el propietario de la compañía)
            if (!appointment.getUser().getId().equals(user.getId()) &&
                    !appointment.getCompany().getOwner().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "No tienes permiso para ver este turno"));
            }

            // 4. Retornar respuesta
            return ResponseEntity.ok(new ApiResponse(true, "Turno obtenido exitosamente", mapToDTO(appointment)));

        } catch (Exception e) {
            log.error("Error al obtener el turno", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al obtener el turno: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getAppointmentsByUser(String userEmail) {
        log.info("Usuario {} solicitando obtener sus turnos", userEmail);

        try {
            // 1. Verificar que el usuario existe
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

            // 2. Buscar los turnos del usuario
            List<Appointment> appointments = appointmentRepository.findByUserId(user.getId());

            // 3. Mapear a DTOs
            List<AppointmentDto> AppointmentDtos = appointments.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            // 4. Retornar respuesta
            return ResponseEntity.ok(new ApiResponse(true, "Turnos obtenidos exitosamente", AppointmentDtos));

        } catch (Exception e) {
            log.error("Error al obtener los turnos del usuario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al obtener los turnos: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getAppointmentsByCompany(String userEmail) {
        log.info("Usuario {} solicitando obtener turnos de su compañía", userEmail);

        try {
            // 1. Verificar que el usuario existe
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

            // 2. Obtener la compañía del usuario
            List<Company> userCompanies = companyRepository.findByOwnerId(user.getId());
            if (userCompanies.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "El usuario no tiene una compañía asociada"));
            }

            Company company = userCompanies.get(0);

            // 3. Buscar los turnos de la compañía
            List<Appointment> appointments = appointmentRepository.findByCompanyId(company.getId());

            // 4. Mapear a DTOs
            List<AppointmentDto> AppointmentDtos = appointments.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            // 5. Retornar respuesta
            return ResponseEntity.ok(new ApiResponse(true, "Turnos obtenidos exitosamente", AppointmentDtos));

        } catch (Exception e) {
            log.error("Error al obtener los turnos de la compañía", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al obtener los turnos: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> updateAppointmentStatus(Long id, UpdateAppointmentStatusRequest request, String userEmail) {
        log.info("Usuario {} solicitando actualizar estado de turno con ID: {}", userEmail, id);

        try {
            // 1. Verificar que el usuario existe
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

            // 2. Buscar el turno
            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

            // 3. Verificar que el usuario tiene permiso para actualizar este turno
            // (solo el propietario de la compañía puede cambiar el estado)
            if (!appointment.getCompany().getOwner().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "No tienes permiso para actualizar este turno"));
            }

            // 4. Actualizar el estado
            appointment.setStatus(request.getStatus());

            // 5. Guardar los cambios
            Appointment updatedAppointment = appointmentRepository.save(appointment);

            // 6. Retornar respuesta
            return ResponseEntity.ok(new ApiResponse(true, "Estado del turno actualizado exitosamente", mapToDTO(updatedAppointment)));

        } catch (Exception e) {
            log.error("Error al actualizar el estado del turno", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al actualizar el estado del turno: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> cancelAppointment(Long id, String userEmail) {
        log.info("Usuario {} solicitando cancelar turno con ID: {}", userEmail, id);

        try {
            // 1. Verificar que el usuario existe
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

            // 2. Buscar el turno
            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

            // 3. Verificar que el usuario tiene permiso para cancelar este turno
            // (es el usuario del turno o el propietario de la compañía)
            boolean isOwner = appointment.getCompany().getOwner().getId().equals(user.getId());
            boolean isAppointmentUser = appointment.getUser().getId().equals(user.getId());

            if (!isOwner && !isAppointmentUser) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "No tienes permiso para cancelar este turno"));
            }

            // 4. Verificar política de cancelación (solo para usuarios, no para propietarios)
            if (isAppointmentUser && !isOwner) {
                try {
                    validateCancellationPolicy(appointment);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ApiResponse(false, e.getMessage()));
                }
            }
            // 5. Primero se devuelve el dinero a traves del MPService antes de actualizar el estado del turno

            mercadoPagoService.refundPayment(appointment.getPaymentId());

            // 6. Actualizar el estado a CANCELLED
            appointment.setStatus(AppointmentStatus.CANCELLED);

            // 7. Guardar los cambios
            Appointment cancelledAppointment = appointmentRepository.save(appointment);

            // 8. Enviar emails de cancelación (asíncrono - no bloquea el flujo principal)
            try {
                emailService.sendAppointmentCancellationToUser(cancelledAppointment)
                    .exceptionally(throwable -> {
                        log.error("Error asíncrono al enviar email de cancelación al usuario para turno ID: {}. Error: {}",
                                 cancelledAppointment.getId(), throwable.getMessage(), throwable);
                        return null;
                    });
                
                emailService.sendAppointmentCancellationToCompany(cancelledAppointment)
                    .exceptionally(throwable -> {
                        log.error("Error asíncrono al enviar email de cancelación a la empresa para turno ID: {}. Error: {}",
                                 cancelledAppointment.getId(), throwable.getMessage(), throwable);
                        return null;
                    });
                
                log.info("Emails de cancelación programados para envío asíncrono para el turno ID: {}", cancelledAppointment.getId());
            } catch (Exception e) {
                log.error("Error al programar envío de emails de cancelación para el turno ID: {}. Error: {}",
                         cancelledAppointment.getId(), e.getMessage(), e);
                // No fallar la cancelación del turno si falla el envío de email
            }

            // 9. Retornar respuesta
            return ResponseEntity.ok(new ApiResponse(true, "Turno cancelado exitosamente", mapToDTO(cancelledAppointment)));

        } catch (Exception e) {
            log.error("Error al cancelar el turno", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al cancelar el turno: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getAvailableSlots(Long serviceId, LocalDate date, String userEmail) {
        log.info("Usuario {} solicitando horarios disponibles para servicio {} en fecha {}", userEmail, serviceId, date);

        try {
            // 1. Verificar que el usuario existe
            userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

            // 2. Obtener el servicio
            BusinessService service = businessServiceRepository.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

            // 3. Obtener la compañía asociada al servicio
            Company company = service.getCompany();

            // 4. Verificar que la fecha cumple con la política de anticipación
            validateDatePolicy(company, date);

            // 5. Obtener los horarios de trabajo para el día de la semana
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            List<BusinessHours> businessHours = businessHoursRepository
                    .findByCompanyIdAndDayOfWeek(company.getId(), dayOfWeek);

            if (businessHours.isEmpty() || !businessHours.get(0).isWorkingDay()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "No hay horarios de atención para este día"));
            }

            // 6. Obtener los turnos existentes para ese día
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            List<Appointment> existingAppointments = appointmentRepository
                    .findByCompanyIdAndStartTimeBetweenAndStatusNot(
                            company.getId(), startOfDay, endOfDay, AppointmentStatus.CANCELLED);

            // 7. Calcular los horarios disponibles
            List<LocalTime> availableSlots = calculateAvailableSlots(
                    businessHours,
                    existingAppointments,
                    service.getDurationMinutes(),
                    company.getHasTimeBetweenTurns() ? company.getMinutesBetweenTurns() : 0,
                    date
            );

            // 7.1 Filtrar horarios pasados si la fecha es hoy (respetando zona horaria)
            java.time.ZoneId zone =  java.time.ZoneId.of("America/Argentina/Buenos_Aires");

            LocalDate today = LocalDate.now(zone);
            if (date.equals(today)) {
                LocalTime nowTime = LocalTime.now(zone);
                availableSlots = availableSlots.stream()
                        .filter(t -> t.isAfter(nowTime))
                        .collect(java.util.stream.Collectors.toList());
            }

            // 8. Retornar respuesta
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Horarios disponibles obtenidos exitosamente",
                    new AvailabilityResponse(availableSlots)
            ));

        } catch (Exception e) {
            log.error("Error al obtener los horarios disponibles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al obtener los horarios disponibles: " + e.getMessage()));
        }
    }

    private void validateAppointmentRequest(Company company, BusinessService service, LocalDateTime startTime, LocalDateTime endTime) {
        // 1. Verificar que la fecha cumple con la política de anticipación
        validateDatePolicy(company, startTime.toLocalDate());

        // 2. Verificar que el día es un día laboral
        DayOfWeek dayOfWeek = startTime.getDayOfWeek();
        List<BusinessHours> businessHours = businessHoursRepository.findByCompanyIdAndDayOfWeek(company.getId(), dayOfWeek);

        if (businessHours.isEmpty() || !businessHours.get(0).isWorkingDay()) {
            throw new RuntimeException("No hay atención en el día seleccionado");
        }

        // 3. Verificar que el horario está dentro del horario de trabajo
        BusinessHours workingHours = businessHours.get(0);
        LocalTime openingTime = workingHours.getOpeningTime();
        LocalTime closingTime = workingHours.getClosingTime();

        if (startTime.toLocalTime().isBefore(openingTime) || endTime.toLocalTime().isAfter(closingTime)) {
            throw new RuntimeException("El horario seleccionado está fuera del horario de atención");
        }

        // 4. Verificar que no hay superposición con otros turnos
        List<Appointment> overlappingAppointments = appointmentRepository.findOverlappingAppointments(
                company.getId(), startTime, endTime,AppointmentStatus.CANCELLED);

        if (!overlappingAppointments.isEmpty()) {
            throw new RuntimeException("El horario seleccionado se superpone con otro turno");
        }

        // 5. Verificar el tiempo entre turnos si es necesario
        if (company.getHasTimeBetweenTurns() && company.getMinutesBetweenTurns() > 0) {
            int minutesBetweenTurns = company.getMinutesBetweenTurns();

            // Buscar turnos cercanos
            LocalDateTime bufferStart = startTime.minusMinutes(minutesBetweenTurns);
            LocalDateTime bufferEnd = endTime.plusMinutes(minutesBetweenTurns);

            List<Appointment> nearbyAppointments = appointmentRepository.findNearbyAppointments(
                    company.getId(), bufferStart, bufferEnd, AppointmentStatus.CANCELLED);

            if (!nearbyAppointments.isEmpty()) {
                throw new RuntimeException("No se respeta el tiempo mínimo entre turnos");
            }
        }
    }

    private void validateDatePolicy(Company company, LocalDate date) {
        LocalDate today = LocalDate.now();

        // Verificar mínimo de días de anticipación
        LocalDate minDate = today.plusDays(company.getMinAdvanceDays());
        if (date.isBefore(minDate)) {
            throw new RuntimeException("La reserva debe hacerse con al menos " +
                    company.getMinAdvanceDays() + " días de anticipación");
        }

        // Verificar máximo de días de anticipación
        LocalDate maxDate = today.plusDays(company.getMaxAdvanceDays());
        if (date.isAfter(maxDate)) {
            throw new RuntimeException("La reserva no puede hacerse con más de " +
                    company.getMaxAdvanceDays() + " días de anticipación");
        }
    }

    private void validateCancellationPolicy(Appointment appointment) {
        // Verificar política de cancelación
        Company company = appointment.getCompany();
        int cancellationHours = company.getCancellationHours();

        LocalDateTime cancellationDeadline = appointment.getStartTime().minusHours(cancellationHours);
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(cancellationDeadline)) {
            throw new RuntimeException("No se puede cancelar el turno con menos de " +
                    cancellationHours + " horas de anticipación");
        }
    }

    private List<LocalTime> calculateAvailableSlots(
            List<BusinessHours> businessHours,
            List<Appointment> existingAppointments,
            int serviceDuration,
            int minutesBetweenTurns,
            LocalDate date) {

        // Para simplificar, tomamos el primer horario de trabajo (podría haber varios por día)
        BusinessHours workingHours = businessHours.get(0);

        LocalTime openingTime = workingHours.getOpeningTime();
        LocalTime closingTime = workingHours.getClosingTime();

        // Convertir a LocalDateTime para manejar correctamente el día
        LocalDateTime startDateTime = date.atTime(openingTime);
        LocalDateTime endDateTime;

        // Si el horario de cierre es 00:00, significa el final del día
        if (closingTime.equals(LocalTime.MIDNIGHT) || closingTime.equals(LocalTime.of(0, 0))) {
            endDateTime = date.plusDays(1).atStartOfDay();
        } else if (closingTime.isBefore(openingTime)) {
            // Si el horario de cierre es anterior al de apertura, asumimos que es del día siguiente
            endDateTime = date.plusDays(1).atTime(closingTime);
        } else {
            endDateTime = date.atTime(closingTime);
        }

        // Lista para almacenar los slots disponibles
        List<LocalTime> availableSlots = new ArrayList<>();

        // Calcular el último horario posible para iniciar un servicio
        LocalDateTime lastPossibleStart = endDateTime.minusMinutes(serviceDuration);

        // Verificar si hay slots disponibles
        if (startDateTime.isAfter(lastPossibleStart)) {
            log.warn("No hay slots disponibles para este día y servicio");
            return availableSlots; // Retornar lista vacía
        }

        // Iterar por cada slot posible basado en la duración del servicio
        LocalDateTime currentDateTime = startDateTime;

        while (!currentDateTime.isAfter(lastPossibleStart)) {
            boolean isAvailable = true;

            // Verificar si el slot se superpone con algún turno existente
            LocalDateTime slotEnd = currentDateTime.plusMinutes(serviceDuration);

            for (Appointment appointment : existingAppointments) {
                // Un slot se superpone si no termina antes o comienza después del turno existente
                if (!(slotEnd.compareTo(appointment.getStartTime()) <= 0 ||
                        currentDateTime.compareTo(appointment.getEndTime()) >= 0)) {
                    isAvailable = false;
                    break;
                }

                // Verificar el tiempo entre turnos si es necesario
                if (minutesBetweenTurns > 0) {
                    // Verificar buffer después del turno existente
                    LocalDateTime bufferStart = appointment.getEndTime();
                    LocalDateTime bufferEnd = appointment.getEndTime().plusMinutes(minutesBetweenTurns);

                    if (!(currentDateTime.compareTo(bufferEnd) >= 0 || slotEnd.compareTo(bufferStart) <= 0)) {
                        isAvailable = false;
                        break;
                    }

                    // Verificar buffer antes del turno existente
                    bufferStart = appointment.getStartTime().minusMinutes(minutesBetweenTurns);
                    bufferEnd = appointment.getStartTime();

                    if (!(currentDateTime.compareTo(bufferEnd) >= 0 || slotEnd.compareTo(bufferStart) <= 0)) {
                        isAvailable = false;
                        break;
                    }
                }
            }

            if (isAvailable) {
                availableSlots.add(currentDateTime.toLocalTime());
            }

            // Avanzar al siguiente slot basado en la duración del servicio
            currentDateTime = currentDateTime.plusMinutes(serviceDuration);

            // Verificación adicional para evitar bucles infinitos
            if (availableSlots.size() > 100) {
                log.warn("Se alcanzó el límite máximo de slots disponibles (100)");
                break;
            }
        }

        return availableSlots;
    }

    private AppointmentDto mapToDTO(Appointment appointment) {
        AppointmentDto dto = new AppointmentDto();
        dto.setId(appointment.getId());
        dto.setServiceId(appointment.getService().getId());
        dto.setServiceName(appointment.getService().getName());
        dto.setUserId(appointment.getUser().getId());
        dto.setUserEmail(appointment.getUser().getEmail());
        dto.setCompanyId(appointment.getCompany().getId());
        dto.setCompanyName(appointment.getCompany().getName());
        dto.setStartTime(appointment.getStartTime());
        dto.setEndTime(appointment.getEndTime());
        dto.setStatus(appointment.getStatus());
        dto.setNotes(appointment.getNotes());
        dto.setCreatedAt(appointment.getCreatedAt());
        dto.setUpdatedAt(appointment.getUpdatedAt());
        return dto;
    }
}