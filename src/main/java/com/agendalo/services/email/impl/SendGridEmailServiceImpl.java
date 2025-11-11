package com.agendalo.services.email.impl;

import com.agendalo.domain.Appointment;
import com.agendalo.domain.Company;
import com.agendalo.domain.User;
import com.agendalo.services.email.EmailService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implementación del servicio de email usando SendGrid API
 * Todos los métodos son asíncronos para no bloquear el flujo principal
 */
@Service
@Primary
@Slf4j
public class SendGridEmailServiceImpl implements EmailService {

    @Autowired
    private SendGrid sendGrid;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    @Value("${sendgrid.from.name}")
    private String fromName;

    @Value("${sendgrid.api.timeout:30000}")
    private int timeout;

    @Value("${sendgrid.api.retry.attempts:3}")
    private int maxRetryAttempts;

    /**
     * Método helper para enviar email con retry logic usando SendGrid API
     */
    private void sendEmailWithRetry(Mail mail, String emailType, String recipientEmail) {
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                log.info("Intento {} de {} para enviar {} a: {} (SendGrid API)", 
                    attempt, maxRetryAttempts, emailType, recipientEmail);
                
                long startTime = System.currentTimeMillis();
                
                Request request = new Request();
                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                request.setBody(mail.build());
                
                Response response = sendGrid.api(request);
                long endTime = System.currentTimeMillis();
                
                if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                    log.info("Email {} enviado exitosamente a {} en {}ms (intento {}) - Status: {}", 
                        emailType, recipientEmail, (endTime - startTime), attempt, response.getStatusCode());
                    return;
                } else {
                    log.warn("Error en intento {} de {} para enviar {} a {} - Status: {} - Body: {}", 
                        attempt, maxRetryAttempts, emailType, recipientEmail, 
                        response.getStatusCode(), response.getBody());
                    
                    if (attempt == maxRetryAttempts) {
                        log.error("Falló el envío de {} a {} después de {} intentos. Status final: {} - Body: {}", 
                            emailType, recipientEmail, maxRetryAttempts, 
                            response.getStatusCode(), response.getBody());
                    }
                }
                
            } catch (IOException e) {
                log.warn("Error de conexión en intento {} de {} para enviar {} a {}: {}", 
                    attempt, maxRetryAttempts, emailType, recipientEmail, e.getMessage());
                
                if (attempt == maxRetryAttempts) {
                    log.error("Falló el envío de {} a {} después de {} intentos. Error final: {}", 
                        emailType, recipientEmail, maxRetryAttempts, e.getMessage(), e);
                }
            } catch (Exception e) {
                log.warn("Error inesperado en intento {} de {} para enviar {} a {}: {}", 
                    attempt, maxRetryAttempts, emailType, recipientEmail, e.getMessage());
                
                if (attempt == maxRetryAttempts) {
                    log.error("Falló el envío de {} a {} después de {} intentos. Error final: {}", 
                        emailType, recipientEmail, maxRetryAttempts, e.getMessage(), e);
                }
            }
            
            if (attempt < maxRetryAttempts) {
                try {
                    long delay = 2000 * attempt; // Backoff exponencial
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupción durante el retry de email");
                    return;
                }
            }
        }
    }

    /**
     * Método helper para crear un Mail object
     */
    private Mail createMail(String toEmail, String subject, String htmlContent) {
        Email from = new Email(fromEmail, fromName);
        Email to = new Email(toEmail);
        Content content = new Content("text/html", htmlContent);
        
        return new Mail(from, subject, to, content);
    }

    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendWelcomeEmail(User user) {
        try {
            log.info("Preparando email de bienvenida al usuario: {} (SendGrid API)", user.getEmail());
            
            // Preparar el contexto para la plantilla
            Context context = new Context();
            context.setVariable("user", user);
            
            // Procesar la plantilla
            String htmlContent = templateEngine.process("welcome-user", context);
            
            Mail mail = createMail(user.getEmail(), "¡Bienvenido a MasDeporte!", htmlContent);
            sendEmailWithRetry(mail, "bienvenida", user.getEmail());
            
        } catch (Exception e) {
            log.error("Error al preparar email de bienvenida al usuario: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendAppointmentConfirmationToUser(Appointment appointment) {
        try {
            log.info("Preparando email de confirmación al usuario: {} (SendGrid API)", appointment.getUser().getEmail());
            
            // Preparar el contexto para la plantilla
            Context context = new Context();
            context.setVariable("appointment", appointment);
            
            // Procesar la plantilla
            String htmlContent = templateEngine.process("appointment-confirmation-user", context);
            
            Mail mail = createMail(appointment.getUser().getEmail(), "Confirmación de Turno - Agendalo", htmlContent);
            sendEmailWithRetry(mail, "confirmación de turno", appointment.getUser().getEmail());
            
        } catch (Exception e) {
            log.error("Error al preparar email de confirmación al usuario: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendAppointmentNotificationToCompany(Appointment appointment) {
        try {
            log.info("Preparando email de notificación a la empresa: {} (SendGrid API)", 
                appointment.getCompany().getOwner().getEmail());

            // Verificaciones de seguridad
            if (appointment.getCompany() == null || appointment.getCompany().getOwner() == null) {
                log.error("La empresa o el propietario del appointment es null");
                return CompletableFuture.completedFuture(null);
            }

            // Preparar el contexto para la plantilla
            Context context = new Context();
            context.setVariable("appointment", appointment);

            // Procesar la plantilla
            String htmlContent = templateEngine.process("appointment-notification-company", context);
            
            Mail mail = createMail(appointment.getCompany().getOwner().getEmail(), 
                "Notificacion de Turno - MasDeporte", htmlContent);
            sendEmailWithRetry(mail, "notificación de turno", appointment.getCompany().getOwner().getEmail());

        } catch (Exception e) {
            log.error("Error al preparar email de notificación a la empresa: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendAppointmentCancellationToUser(Appointment appointment) {
        try {
            log.info("Preparando email de cancelación al usuario: {} (SendGrid API)", appointment.getUser().getEmail());
            
            // Preparar el contexto para la plantilla
            Context context = new Context();
            context.setVariable("appointment", appointment);

            // Procesar la plantilla
            String htmlContent = templateEngine.process("appointment-cancellation-user", context);
            
            Mail mail = createMail(appointment.getUser().getEmail(), "Turno Cancelado - Agendalo", htmlContent);
            sendEmailWithRetry(mail, "cancelación de turno", appointment.getUser().getEmail());
            
        } catch (Exception e) {
            log.error("Error al preparar email de cancelación al usuario: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendAppointmentCancellationToCompany(Appointment appointment) {
        try {
            log.info("Preparando email de cancelación a la empresa: {} (SendGrid API)", 
                appointment.getCompany().getOwner().getEmail());
            
            // Preparar el contexto para la plantilla
            Context context = new Context();
            context.setVariable("appointment", appointment);
            
            // Procesar la plantilla
            String htmlContent = templateEngine.process("appointment-cancellation-company", context);
            
            Mail mail = createMail(appointment.getCompany().getOwner().getEmail(), 
                "Turno Cancelado - Agendalo", htmlContent);
            sendEmailWithRetry(mail, "cancelación de turno", appointment.getCompany().getOwner().getEmail());
            
        } catch (Exception e) {
            log.error("Error al preparar email de cancelación a la empresa: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendCompanyCancellationEmail(Company company, String reason) {
        try {
            log.info("Preparando email de cancelación de empresa: {} (SendGrid API)", company.getOwner().getEmail());

            // Preparar el contexto para la plantilla
            Context context = new Context();
            context.setVariable("company", company);
            context.setVariable("reason", reason);
            
            // Procesar la plantilla
            String htmlContent = templateEngine.process("company-cancellation", context);
            
            Mail mail = createMail(company.getOwner().getEmail(), 
                "Solicitud de creación de club cancelada - Agendalo", htmlContent);
            sendEmailWithRetry(mail, "cancelación de empresa", company.getOwner().getEmail());

        } catch (Exception e) {
            log.error("Error al preparar email de cancelación a la empresa: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendCompanyApproveEmail(Company company) {
        try {
            log.info("Preparando email de aprobacion de empresa: {} (SendGrid API)", company.getOwner().getEmail());

            // Preparar el contexto para la plantilla
            Context context = new Context();
            context.setVariable("company", company);
            
            // Procesar la plantilla
            String htmlContent = templateEngine.process("company-creation", context);
            
            Mail mail = createMail(company.getOwner().getEmail(), 
                "Solicitud de creación de club aprobada - Agendalo", htmlContent);
            sendEmailWithRetry(mail, "aprobación de empresa", company.getOwner().getEmail());

        } catch (Exception e) {
            log.error("Error al preparar email de aprobacion a la empresa: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(null);
    }
}