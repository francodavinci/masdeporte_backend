package com.agendalo.controller.mercado_pago;

import com.agendalo.domain.MercadoPagoAccount;
import com.agendalo.domain.User;
import com.agendalo.dto.payment.PaymentPreferenceRequest;
import com.agendalo.dto.payment.PaymentStatusResponse;
import com.agendalo.dto.response.ApiResponse;
import com.agendalo.exceptions.MercadoPagoException;
import com.agendalo.services.mercado_pago.MercadoPagoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/mercadopago")
public class MercadoPagoController {

    private final MercadoPagoService mercadoPagoService;

    public MercadoPagoController(MercadoPagoService mercadoPagoService) {
        this.mercadoPagoService = mercadoPagoService;
    }

    @PostMapping("/oauth/callback")
    public ResponseEntity<?> connectAccount(@RequestBody Map<String, String> body) {
        log.info("in callback");
        String code = body.get("code");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        MercadoPagoAccount account = mercadoPagoService.connectAccount(email, code);
        return ResponseEntity.ok(new ApiResponse(true, "Cuenta conectada exitosamente", account));
    }

    @GetMapping("/oauth/status")
    public ResponseEntity<?> getStatus(Authentication authentication) {
        String email = authentication.getName();
        Map<String, Object> status = mercadoPagoService.getStatus(email);
        return ResponseEntity.ok(new ApiResponse(true, "Estado obtenido exitosamente", status));
    }

    @DeleteMapping("/oauth/disconnect")
    public ResponseEntity<?> disconnectAccount(Authentication authentication) {
        String email = authentication.getName();
        mercadoPagoService.disconnectAccount(email);
        return ResponseEntity.ok(new ApiResponse(true, "Cuenta desconectada exitosamente"));
    }

    @PostMapping("/preferences")
    public ResponseEntity<?> createPaymentPreference(
            @RequestBody PaymentPreferenceRequest request,
            Authentication authentication) {
        try {
            Map<String, Object> preference = mercadoPagoService.createPaymentPreference(
                    authentication.getName(),
                    request
            );
            return ResponseEntity.ok(new ApiResponse(true, "Preferencia de pago creada exitosamente", preference));
        } catch (MercadoPagoException e) {
            log.error("Error en createPaymentPreference: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> webhookData) {
        try {
            log.info("webhook initialize");
            mercadoPagoService.processWebhook(webhookData);
            return ResponseEntity.ok(new ApiResponse(true, "Webhook procesado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Error procesando webhook: " + e.getMessage()));
        }
    }

    @PostMapping("/refund/{paymentId}")
    public ResponseEntity<?> refundPayment(@PathVariable String paymentId) {
        try {
            Map<String, Object> paymentInfo = mercadoPagoService.refundPayment(paymentId);
            return ResponseEntity.ok(new ApiResponse(true, "Devolucion de pago realizada exitosamente", paymentInfo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Error creando la devolucion del pago: " + e.getMessage()));
        }
    }

    @GetMapping("/payment/{paymentId}/status")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String paymentId) {
        try {
            Map<String, Object> paymentInfo = mercadoPagoService.getPaymentInfo(paymentId);
            return ResponseEntity.ok(new ApiResponse(true, "Estado del pago obtenido exitosamente", paymentInfo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Error obteniendo estado del pago: " + e.getMessage()));
        }
    }

    @GetMapping("/preferences")
    public ResponseEntity<?> getUserPaymentPreferences(Authentication authentication) {
        try {
            String email = authentication.getName();
            var preferences = mercadoPagoService.getUserPaymentPreferences(email);
            return ResponseEntity.ok(new ApiResponse(true, "Preferencias obtenidas exitosamente", preferences));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Error obteniendo preferencias: " + e.getMessage()));
        }
    }

    @GetMapping("/preferences/{preferenceId}")
    public ResponseEntity<?> getPaymentPreferenceById(@PathVariable String preferenceId) {
        try {
            var preference = mercadoPagoService.getPaymentPreferenceById(preferenceId);
            return ResponseEntity.ok(new ApiResponse(true, "Preferencia obtenida exitosamente", preference));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Error obteniendo preferencia: " + e.getMessage()));
        }
    }

    @GetMapping("/appointments/pending-payment")
    public ResponseEntity<?> getPendingPaymentAppointments(Authentication authentication) {
        try {
            String email = authentication.getName();
            var appointments = mercadoPagoService.getPendingPaymentAppointments(email);
            return ResponseEntity.ok(new ApiResponse(true, "Reservas pendientes de pago obtenidas exitosamente", appointments));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Error obteniendo reservas pendientes: " + e.getMessage()));
        }
    }

    @PostMapping("/preferences/{preferenceId}/cancel")
    public ResponseEntity<?> cancelPaymentPreference(@PathVariable String preferenceId, Authentication authentication) {
        try {
            String email = authentication.getName();
            mercadoPagoService.cancelPaymentPreference(preferenceId, email);
            return ResponseEntity.ok(new ApiResponse(true, "Preferencia cancelada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Error cancelando preferencia: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getPaymentStatistics(Authentication authentication) {
        try {
            String email = authentication.getName();
            var statistics = mercadoPagoService.getPaymentStatistics(email);
            return ResponseEntity.ok(new ApiResponse(true, "Estadísticas obtenidas exitosamente", statistics));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Error obteniendo estadísticas: " + e.getMessage()));
        }
    }

    @GetMapping("/test-connection")
    public ResponseEntity<?> testConnection(Authentication authentication) {
        try {
            String email = authentication.getName();
            var status = mercadoPagoService.getStatus(email);
            return ResponseEntity.ok(new ApiResponse(true, "Conexión probada exitosamente", status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Error probando conexión: " + e.getMessage()));
        }
    }

    @GetMapping("/preferences/example")
    public ResponseEntity<?> getPaymentPreferenceExample() {
        PaymentPreferenceRequest example = new PaymentPreferenceRequest();
        example.setTitle("Servicio de Consulta");
        example.setDescription("Consulta médica de 30 minutos");
        example.setAmount(5000.0);
        example.setQuantity(1);
        example.setCurrency("ARS");
        example.setServiceId(1L); // ID del servicio en la base de datos
        example.setUserId(1L); // ID del usuario que hace la reserva
        example.setStartTime("2024-01-15T10:00:00"); // Formato ISO
        example.setNotes("Consulta de rutina");
        example.setUserEmail("usuario@ejemplo.com"); // Email del usuario que hace la reserva
        
        return ResponseEntity.ok(new ApiResponse(true, "Ejemplo de request para crear preferencia de pago", example));
    }

    @PostMapping("/preferences/test")
    public ResponseEntity<?> createTestPaymentPreference(Authentication authentication) {
        try {
            // Crear una request de prueba con datos mínimos
            PaymentPreferenceRequest testRequest = new PaymentPreferenceRequest();
            testRequest.setTitle("Test Service");
            testRequest.setDescription("Test Description");
            testRequest.setAmount(100.0);
            testRequest.setQuantity(1);
            testRequest.setCurrency("ARS");
            testRequest.setServiceId(1L); // Asegúrate de que este ID existe en tu base de datos
            testRequest.setUserId(1L);
            testRequest.setStartTime("2024-12-20T10:00:00");
            testRequest.setNotes("Test appointment");
            testRequest.setUserEmail("test@example.com"); // Asegúrate de que este email existe en tu base de datos
            
            log.info("Creando preferencia de prueba: {}", testRequest);
            
            Map<String, Object> preference = mercadoPagoService.createPaymentPreference(
                    authentication.getName(),
                    testRequest
            );
            return ResponseEntity.ok(new ApiResponse(true, "Preferencia de prueba creada exitosamente", preference));
        } catch (Exception e) {
            log.error("Error creando preferencia de prueba: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Error creando preferencia de prueba: " + e.getMessage()));
        }
    }
}