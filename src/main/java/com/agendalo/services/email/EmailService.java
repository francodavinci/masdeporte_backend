package com.agendalo.services.email;

import com.agendalo.domain.Appointment;
import com.agendalo.domain.Company;
import com.agendalo.domain.User;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

/**
 * Servicio para el envío de emails de forma asíncrona
 * Todos los métodos se ejecutan en hilos separados para no bloquear el flujo principal
 */
public interface EmailService {

    /**
     * Envía un email de bienvenida a un nuevo usuario (asíncrono)
     * @param user El usuario recién registrado
     * @return CompletableFuture que se completa cuando el email se envía
     */
    @Async("emailTaskExecutor")
    CompletableFuture<Void> sendWelcomeEmail(User user);

    /**
     * Envía un email de confirmación de turno al usuario (asíncrono)
     * @param appointment El turno creado
     * @return CompletableFuture que se completa cuando el email se envía
     */
    @Async("emailTaskExecutor")
    CompletableFuture<Void> sendAppointmentConfirmationToUser(Appointment appointment);

    /**
     * Envía un email de notificación de nuevo turno al propietario de la compañía (asíncrono)
     * @param appointment El turno creado
     * @return CompletableFuture que se completa cuando el email se envía
     */
    @Async("emailTaskExecutor")
    CompletableFuture<Void> sendAppointmentNotificationToCompany(Appointment appointment);

    /**
     * Envía un email de cancelación de turno al usuario (asíncrono)
     * @param appointment El turno cancelado
     * @return CompletableFuture que se completa cuando el email se envía
     */
    @Async("emailTaskExecutor")
    CompletableFuture<Void> sendAppointmentCancellationToUser(Appointment appointment);

    /**
     * Envía un email de notificación de cancelación al propietario de la compañía (asíncrono)
     * @param appointment El turno cancelado
     * @return CompletableFuture que se completa cuando el email se envía
     */
    @Async("emailTaskExecutor")
    CompletableFuture<Void> sendAppointmentCancellationToCompany(Appointment appointment);

    /**
     * Envía un email de cancelación de compañía (asíncrono)
     * @param company La compañía cancelada
     * @param reason La razón de la cancelación
     * @return CompletableFuture que se completa cuando el email se envía
     */
    @Async("emailTaskExecutor")
    CompletableFuture<Void> sendCompanyCancellationEmail(Company company, String reason);

    /**
     * Envía un email de aprobación de compañía (asíncrono)
     * @param company La compañía aprobada
     * @return CompletableFuture que se completa cuando el email se envía
     */
    @Async("emailTaskExecutor")
    CompletableFuture<Void> sendCompanyApproveEmail(Company company);

}
