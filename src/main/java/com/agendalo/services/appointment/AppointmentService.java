package com.agendalo.services.appointment;

import com.agendalo.dto.appointment.CreateAppointmentRequest;
import com.agendalo.dto.appointment.UpdateAppointmentStatusRequest;
import com.agendalo.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

public interface AppointmentService {

    /**
     * Crea un nuevo turno
     * @param request Datos del turno a crear
     * @param userEmail Email del usuario que realiza la solicitud
     * @return Respuesta con el turno creado
     */
    ResponseEntity<ApiResponse> createAppointment(CreateAppointmentRequest request, String userEmail);

    /**
     * Obtiene un turno por su ID
     * @param id ID del turno
     * @param userEmail Email del usuario que realiza la solicitud
     * @return Respuesta con el turno solicitado
     */
    ResponseEntity<ApiResponse> getAppointmentById(Long id, String userEmail);

    /**
     * Obtiene todos los turnos de un usuario
     * @param userEmail Email del usuario
     * @return Respuesta con la lista de turnos
     */
    ResponseEntity<ApiResponse> getAppointmentsByUser(String userEmail);

    /**
     * Obtiene todos los turnos de una compañía
     * @param userEmail Email del propietario de la compañía
     * @return Respuesta con la lista de turnos
     */
    ResponseEntity<ApiResponse> getAppointmentsByCompany(String userEmail);

    /**
     * Actualiza el estado de un turno
     * @param id ID del turno
     * @param request Nuevo estado
     * @param userEmail Email del usuario que realiza la solicitud
     * @return Respuesta con el turno actualizado
     */
    ResponseEntity<ApiResponse> updateAppointmentStatus(Long id, UpdateAppointmentStatusRequest request, String userEmail);

    /**
     * Cancela un turno
     * @param id ID del turno
     * @param userEmail Email del usuario que realiza la solicitud
     * @return Respuesta con el resultado de la operación
     */
    ResponseEntity<ApiResponse> cancelAppointment(Long id, String userEmail);

    /**
     * Obtiene los horarios disponibles para un servicio en una fecha específica
     * @param serviceId ID del servicio
     * @param date Fecha
     * @param userEmail Email del usuario que realiza la solicitud
     * @return Respuesta con los horarios disponibles
     */
    ResponseEntity<ApiResponse> getAvailableSlots(Long serviceId, LocalDate date, String userEmail);
}