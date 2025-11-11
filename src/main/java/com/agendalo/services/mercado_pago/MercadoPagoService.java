package com.agendalo.services.mercado_pago;

import com.agendalo.domain.*;
import com.agendalo.domain.enums.AppointmentStatus;
import com.agendalo.dto.payment.PaymentPreferenceRequest;
import com.agendalo.exceptions.MercadoPagoException;
import com.agendalo.exceptions.ResourceNotFoundException;
import com.agendalo.repository.*;
import com.agendalo.services.calendar.GoogleCalendarService;
import com.agendalo.services.email.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class MercadoPagoService {

    @Value("${mercadopago.client.id}")
    private String clientId;

    @Value("${mercadopago.client.secret}")
    private String clientSecret;

    @Value("${mercadopago.redirect.uri}")
    private String redirectUri;

    @Value("${mercadopago.api.base-url}")
    private String baseUrl;

    @Value("${mercadopago.api.oauth-token-path}")
    private String oauthTokenPath;
    @Value("${mercadopago.back-urls.success}")
    private String successUrl;

    @Value("${mercadopago.back-urls.failure}")
    private String failureUrl;

    @Value("${mercadopago.back-urls.pending}")
    private String pendingUrl;

    @Value("${mercadopago.notification-url}")
    private String notificationUrl;

    @Value("${encryption.key}")
    private String encryptionKey;

    private final MercadoPagoAccountRepository mpAccountRepo;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PaymentPreferenceRepository paymentPreferenceRepository;
    private final BusinessServiceRepository businessServiceRepository;
    private final AppointmentRepository appointmentRepository;
    private final GoogleCalendarService googleCalendarService;
    private final EmailService emailService;
    public MercadoPagoService(MercadoPagoAccountRepository mpAccountRepo, 
                             CompanyRepository companyRepo, 
                             UserRepository userRepository,
                             PaymentPreferenceRepository paymentPreferenceRepository,
                             BusinessServiceRepository businessServiceRepository,
                             AppointmentRepository appointmentRepository,
                              GoogleCalendarService googleCalendarService,
                              EmailService emailService) {
        this.mpAccountRepo = mpAccountRepo;
        this.companyRepository = companyRepo;
        this.userRepository = userRepository;
        this.paymentPreferenceRepository = paymentPreferenceRepository;
        this.businessServiceRepository = businessServiceRepository;
        this.appointmentRepository = appointmentRepository;
        this.googleCalendarService = googleCalendarService;
        this.emailService = emailService;
    }

    private void validateAccessToken(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.mercadopago.com/users/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new MercadoPagoException("Token inválido o expirado");
            }
        } catch (Exception e) {
            throw new MercadoPagoException("Error validando token: " + e.getMessage());
        }
    }

    private void refreshAccessToken(MercadoPagoAccount account) {
        if (account.getRefreshToken() == null) {
            throw new MercadoPagoException("No hay token de refresco disponible");
        }

        RestTemplate restTemplate = new RestTemplate();
        String url = baseUrl + oauthTokenPath;

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", account.getRefreshToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new MercadoPagoException("Error al refrescar el token");
            }

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new MercadoPagoException("Respuesta inválida al refrescar token");
            }

            account.setAccessToken((String) responseBody.get("access_token"));
            account.setRefreshToken((String) responseBody.get("refresh_token"));
            
            if (responseBody.get("expires_in") != null) {
                int expiresIn = Integer.parseInt(responseBody.get("expires_in").toString());
                account.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
            } else {
                account.setExpiresAt(LocalDateTime.now().plusHours(6));
            }

            mpAccountRepo.save(account);
        } catch (Exception e) {
            throw new MercadoPagoException("Error refrescando token: " + e.getMessage());
        }
    }
    private String encryptToken(String token) {
        try {
            SecretKey key = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(token.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new SecurityException("Error encriptando token", e);
        }
    }

    private String decryptToken(String encryptedToken) {
        try {
            SecretKey key = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedToken));
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new SecurityException("Error desencriptando token", e);
        }
    }
    public MercadoPagoAccount connectAccount(String email, String code) {
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));

        Company company = companyRepository.findByOwnerId(owner.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una empresa para el usuario"));

        // 1. Intercambiar el code por el access_token
        RestTemplate restTemplate = new RestTemplate();
        String url = baseUrl + oauthTokenPath;

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new MercadoPagoException("Error al conectar con Mercado Pago: " +
                        (response.getBody() != null ? response.getBody().toString() : "Error desconocido"));
            }

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new MercadoPagoException("Respuesta inválida de Mercado Pago");
            }

            // 2. Guardar los datos en la base de datos
            Long companyId = company.getId();
            MercadoPagoAccount account = mpAccountRepo.findByCompanyId(companyId);

            if (account == null) {
                account = new MercadoPagoAccount();
                account.setCompany(company);
            }

            account.setAccessToken((String) responseBody.get("access_token"));
            account.setRefreshToken((String) responseBody.get("refresh_token"));
            account.setUserId(String.valueOf(responseBody.get("user_id")));
            account.setStatus("active");
            account.setLastSync(LocalDateTime.now());
            
            // Calcular fecha de expiración (los tokens de Mercado Pago expiran en 6 horas)
            if (responseBody.get("expires_in") != null) {
                int expiresIn = Integer.parseInt(responseBody.get("expires_in").toString());
                account.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
            } else {
                // Por defecto, 6 horas
                account.setExpiresAt(LocalDateTime.now().plusHours(6));
            }

            return mpAccountRepo.save(account);

        } catch (RestClientException e) {
            throw new MercadoPagoException("Error de comunicación con Mercado Pago: " + e.getMessage());
        }
    }

    public Map<String, Object> getStatus(String email) {
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));

        Company company = companyRepository.findByOwnerId(owner.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una empresa para el usuario"));

        MercadoPagoAccount account = mpAccountRepo.findByCompanyId(company.getId());

        boolean isConnected = account != null && account.getAccessToken() != null;
        String status = isConnected ? account.getStatus() : "not_connected";

        Map<String, Object> result = new HashMap<>();
        result.put("isConnected", isConnected);
        result.put("status", status);
        return result;
    }

    public void disconnectAccount(String email) {
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));

        Company company = companyRepository.findByOwnerId(owner.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una empresa para el usuario"));

        MercadoPagoAccount account = mpAccountRepo.findByCompanyId(company.getId());
        if (account != null) {
            mpAccountRepo.delete(account);
        }
    }

    @Value("${mercadopago.api.preferences-path}")
    private String preferencesPath;

    public Map<String, Object> createPaymentPreference(String email, PaymentPreferenceRequest request) {
        // 1. Obtener la cuenta de Mercado Pago del usuario
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
        Company company = companyRepository.findById(request.getCompanyId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una empresa para el id enviado"));
        MercadoPagoAccount account = mpAccountRepo.findByCompanyId(company.getId());

        if (account == null || account.getAccessToken() == null) {
            throw new MercadoPagoException("La cuenta no está conectada con Mercado Pago");
        }

        // Validar que el token no esté expirado y refrescarlo si es necesario
        if (account.getExpiresAt() != null && account.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshAccessToken(account);
        }

        // 2. Crear la preferencia de pago
        RestTemplate restTemplate = new RestTemplate();
        String url = baseUrl + preferencesPath;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(account.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 3. Construir el body de la preferencia
        Map<String, Object> preferenceBody = new HashMap<>();
        preferenceBody.put("items", List.of(Map.of(
                "title", request.getTitle(),
                "description", request.getDescription(),
                "quantity", request.getQuantity(),
                "currency_id", request.getCurrency(),
                "unit_price", request.getAmount()
        )));
        Map<String, Object> paymentMethods = new HashMap<>();
        paymentMethods.put("excluded_payment_methods", List.of());  // Permitir todos los métodos
        paymentMethods.put("excluded_payment_types", List.of());    // Permitir todos los tipos
        paymentMethods.put("installments", 12);                     // Hasta 12 cuotas
        paymentMethods.put("default_installments", 1);              // Por defecto 1 cuota
        preferenceBody.put("payment_methods", paymentMethods);

// 8. Configurar auto_return
        preferenceBody.put("auto_return", "approved");

        // 4. Configurar URLs de retorno
        // ✅ NUEVO: Usar las back_urls del request si están presentes, sino usar las por defecto
        Map<String, String> backUrls = new HashMap<>();

        if (request.getBackUrls() != null && !request.getBackUrls().isEmpty()) {
            // Usar las URLs proporcionadas por el cliente (para app móvil)
            backUrls.put("success", request.getBackUrls().getOrDefault("success", successUrl));
            backUrls.put("failure", request.getBackUrls().getOrDefault("failure", failureUrl));
            backUrls.put("pending", request.getBackUrls().getOrDefault("pending", pendingUrl));

            log.info("Usando back_urls del cliente: {}", backUrls);
        } else {
            // Usar URLs por defecto (para web)
            backUrls.put("success", successUrl);
            backUrls.put("failure", failureUrl);
            backUrls.put("pending", pendingUrl);

            log.info("Usando back_urls por defecto: {}", backUrls);
        }

        preferenceBody.put("back_urls", backUrls);

        // 5. Agregar external reference
        preferenceBody.put("external_reference", request.getExternalReference());

        // 6. Configurar notificaciones
        preferenceBody.put("notification_url", notificationUrl);

        // 7. Hacer la petición a Mercado Pago
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(preferenceBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new MercadoPagoException("Error al crear la preferencia de pago: " +
                        (response.getBody() != null ? response.getBody().toString() : "Error desconocido"));
            }

            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null) {
                throw new MercadoPagoException("Respuesta inválida de Mercado Pago");
            }

            // 8. Guardar la preferencia en la base de datos con la información de la reserva
            savePaymentPreference(responseBody, request, company);

            return responseBody;
        } catch (RestClientException e) {
            throw new MercadoPagoException("Error de comunicación con Mercado Pago: " + e.getMessage());
        }
    }
    private void savePaymentPreference(Map<String, Object> mpResponse, PaymentPreferenceRequest request, Company company) {
        // Validar que el serviceId esté presente
        if (request.getServiceId() == null) {
            throw new MercadoPagoException("El serviceId es requerido para crear la preferencia de pago");
        }

        // Validar que el userEmail esté presente
        if (request.getUserEmail() == null || request.getUserEmail().trim().isEmpty()) {
            throw new MercadoPagoException("El userEmail es requerido para crear la preferencia de pago");
        }

        // Validar que el startTime esté presente
        if (request.getStartTime() == null || request.getStartTime().trim().isEmpty()) {
            throw new MercadoPagoException("El startTime es requerido para crear la preferencia de pago");
        }

        // Obtener el servicio
        BusinessService service = businessServiceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con ID: " + request.getServiceId()));

        // Obtener el usuario
        User user = userRepository.findByEmail(request.getUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + request.getUserEmail()));

        // Parsear la fecha de inicio
        LocalDateTime startTime;
        try {
            startTime = LocalDateTime.parse(request.getStartTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            throw new MercadoPagoException("Formato de fecha inválido. Use formato ISO: " + request.getStartTime());
        }

        // Crear y guardar la preferencia
        PaymentPreference preference = new PaymentPreference();
        preference.setPreferenceId((String) mpResponse.get("id"));
        preference.setService(service);
        preference.setUser(user);
        preference.setCompany(company);
        preference.setStartTime(startTime);
        preference.setNotes(request.getNotes());
        preference.setAmount(request.getAmount());
        preference.setExternalReference(request.getExternalReference());
        preference.setStatus("pending");

        paymentPreferenceRepository.save(preference);
    }

    @Transactional
    public void processWebhook(Map<String, Object> webhookData) {
        try {
            // Extraer información del webhook de Mercado Pago
            String topic = (String) webhookData.get("topic");
            String resource = (String) webhookData.get("resource");

            // Mercado Pago envía webhooks con topic "payment" y resource contiene el ID del pago
            if ("payment".equals(topic) && resource != null) {
                // El resource es directamente el ID del pago
                String paymentId = resource;
                processPaymentNotification(paymentId);
            }
        } catch (Exception e) {
            throw new MercadoPagoException("Error procesando webhook: " + e.getMessage());
        }
    }

    private void processPaymentNotification(String paymentId) {
        try {
            System.out.println("Procesando notificación de pago para paymentId: " + paymentId);
            
            // 1. Obtener información del pago desde Mercado Pago usando el token de la empresa
            Map<String, Object> paymentInfo = getPaymentInfoWithCompanyToken(paymentId);
            
            System.out.println("Información del pago obtenida: " + paymentInfo);
            
            // 2. Verificar si el pago fue exitoso
            String status = (String) paymentInfo.get("status");
            System.out.println("Estado del pago: " + status);
            
            if ("approved".equals(status)) {
                // 3. Obtener la preferencia asociada
                String externalReference = (String) paymentInfo.get("external_reference");
                System.out.println("External Reference encontrado: " + externalReference);
                
                if (externalReference == null || externalReference.trim().isEmpty()) {
                    System.err.println("ERROR: externalReferenceId es null o vacío en la respuesta del pago");
                    System.err.println("Claves disponibles en paymentInfo: " + paymentInfo.keySet());
                    return;
                }
                
                PaymentPreference preference = paymentPreferenceRepository.findByExternalReference(externalReference)
                        .orElseThrow(() -> new ResourceNotFoundException("Preferencia no encontrada con ID: " + externalReference));

                System.out.println("Preferencia encontrada: " + preference.getPreferenceId());

                // 4. Crear la reserva automáticamente
                createAppointmentFromPayment(preference, paymentId);
                
                // 5. Actualizar el estado de la preferencia
                preference.setStatus("approved");
                paymentPreferenceRepository.save(preference);
                
                System.out.println("Cita creada exitosamente para el pago: " + paymentId);
            } else {
                System.out.println("Pago no aprobado, estado: " + status);
            }
        } catch (Exception e) {
            // Log del error pero no fallar el webhook
            System.err.println("Error procesando notificación de pago: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public Map<String, Object> refundPayment(String paymentId) {
        // Buscar compania por payment ID

        Appointment appointmentFound = appointmentRepository.findByPaymentId(paymentId).orElseThrow( () -> new RuntimeException("Company not found"));

        // Una vez encontrada la compania buscar el access_token para esa compania en la tabla mercado pago account
        MercadoPagoAccount accountFound = mpAccountRepo.findByCompanyId(appointmentFound.getCompany().getId());

        String accessToken = accountFound.getAccessToken();

        RestTemplate restTemplate = new RestTemplate();
        String url = baseUrl + "/v1/payments/" + paymentId + "/refunds";

        HttpHeaders headers = new HttpHeaders();
        // Para consultas de pagos desde webhooks, usamos el client_secret como token de aplicación
        // TODO : obtener el mercado pago access_token del usuario
        headers.set("Authorization", "Bearer " + accessToken);
        UUID randomUuid = UUID.randomUUID();
        headers.set("X-Idempotency-Key" ,  randomUuid.toString());

        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new MercadoPagoException("Error intentando realizar la devolucion de pago: " +
                        (response.getBody() != null ? response.getBody().toString() : "Error desconocido"));
            }

            return response.getBody();
        } catch (Exception e) {
            throw new MercadoPagoException("Error intentando realizar la devolucion de pago: " + e.getMessage());
        }
    }

    public Map<String, Object> getPaymentInfo(String paymentId) {
        RestTemplate restTemplate = new RestTemplate();
        String url = baseUrl + "/v1/payments/" + paymentId;

        HttpHeaders headers = new HttpHeaders();
        // Para consultas de pagos desde webhooks, usamos el client_secret como token de aplicación
        headers.set("Authorization", "Bearer " + clientSecret);

        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new MercadoPagoException("Error obteniendo información del pago: " + 
                    (response.getBody() != null ? response.getBody().toString() : "Error desconocido"));
            }
            
            return response.getBody();
        } catch (Exception e) {
            throw new MercadoPagoException("Error obteniendo información del pago: " + e.getMessage());
        }
    }

    private Map<String, Object> getPaymentInfoWithCompanyToken(String paymentId) {
        System.out.println("Intentando obtener información del pago: " + paymentId);
        
        // Primero intentamos obtener la información del pago usando el client_secret
        // Si falla, intentamos con los tokens de las empresas conectadas
        try {
            System.out.println("Intentando con client_secret...");
            Map<String, Object> result = getPaymentInfo(paymentId);
            System.out.println("Éxito con client_secret");
            return result;
        } catch (MercadoPagoException e) {
            System.out.println("Falló con client_secret: " + e.getMessage());
            
            // Si falla con client_secret, intentamos con los tokens de las empresas
            List<MercadoPagoAccount> accounts = mpAccountRepo.findAll();
            System.out.println("Intentando con " + accounts.size() + " cuentas de empresa...");
            
            for (MercadoPagoAccount account : accounts) {
                if (account.getAccessToken() != null) {
                    try {
                        System.out.println("Intentando con cuenta de empresa ID: " + account.getId());
                        
                        // Verificar si el token no está expirado
                        if (account.getExpiresAt() != null && account.getExpiresAt().isBefore(LocalDateTime.now())) {
                            System.out.println("Refrescando token expirado...");
                            refreshAccessToken(account);
                        }
                        
                        RestTemplate restTemplate = new RestTemplate();
                        String url = baseUrl + "/v1/payments/" + paymentId;

                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth(account.getAccessToken());

                        HttpEntity<?> request = new HttpEntity<>(headers);

                        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
                        
                        if (response.getStatusCode().is2xxSuccessful()) {
                            System.out.println("Éxito con token de empresa ID: " + account.getId());
                            return response.getBody();
                        } else {
                            System.out.println("Error con token de empresa, status: " + response.getStatusCode());
                        }
                    } catch (Exception ex) {
                        System.out.println("Excepción con token de empresa: " + ex.getMessage());
                        // Continuar con la siguiente cuenta
                        continue;
                    }
                }
            }
            
            // Si llegamos aquí, ningún token funcionó
            throw new MercadoPagoException("No se pudo obtener información del pago con ningún token disponible");
        }
    }

    private void createAppointmentFromPayment(PaymentPreference preference, String paymentId) {
        // Calcular la hora de finalización basada en la duración del servicio
        LocalDateTime startTime = preference.getStartTime();
        LocalDateTime endTime = startTime.plusMinutes(preference.getService().getDurationMinutes());

        // Crear la reserva
        Appointment appointment = new Appointment();
        appointment.setService(preference.getService());
        appointment.setUser(preference.getUser());
        appointment.setCompany(preference.getCompany());
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus(AppointmentStatus.CONFIRMED); // Reserva confirmada por pago exitoso
        appointment.setNotes(preference.getNotes());
        appointment.setPaymentId(paymentId);

        appointmentRepository.save(appointment);

        try {
            if (googleCalendarService.isConfigured(appointment.getCompany().getId())) {
                googleCalendarService.createAppointmentEvent(appointment);
                log.info("Evento creado en Google Calendar para turno: {}", appointment.getId());
            } else {
                log.info("Google Calendar no configurado para la compañía: {}", appointment.getCompany().getId());
            }
        } catch (Exception e) {
            log.error("Error creando evento en Google Calendar para turno: {}", appointment.getId(), e);
            // No fallar la creación del turno si falla el calendario
        }

        try {
            emailService.sendAppointmentConfirmationToUser(appointment)
                .exceptionally(throwable -> {
                    log.error("Error asíncrono al enviar email de confirmación al usuario para turno ID: {}. Error: {}",
                             appointment.getId(), throwable.getMessage(), throwable);
                    return null;
                });
            
            emailService.sendAppointmentNotificationToCompany(appointment)
                .exceptionally(throwable -> {
                    log.error("Error asíncrono al enviar email de notificación a la empresa para turno ID: {}. Error: {}",
                             appointment.getId(), throwable.getMessage(), throwable);
                    return null;
                });
            
            log.info("Emails de notificación programados para envío asíncrono para el turno ID: {}", appointment.getId());
        } catch (Exception e) {
            log.error("Error al programar envío de emails de notificación para el turno ID: {}. Error: {}",
                    appointment.getId(), e.getMessage(), e);
            // No fallar la creación del turno si falla el envío de email
        }

    }

    public List<PaymentPreference> getUserPaymentPreferences(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
        
        return paymentPreferenceRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public PaymentPreference getPaymentPreferenceById(String preferenceId) {
        return paymentPreferenceRepository.findByPreferenceId(preferenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Preferencia no encontrada"));
    }

    public List<PaymentPreference> getPendingPaymentAppointments(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
        
        return paymentPreferenceRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), "pending");
    }

    @Transactional
    public void cancelPaymentPreference(String preferenceId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
        
        PaymentPreference preference = paymentPreferenceRepository.findByPreferenceId(preferenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Preferencia no encontrada"));
        
        // Verificar que la preferencia pertenece al usuario
        if (!preference.getUser().getId().equals(user.getId())) {
            throw new MercadoPagoException("No tienes permisos para cancelar esta preferencia");
        }
        
        // Solo permitir cancelar preferencias pendientes
        if (!"pending".equals(preference.getStatus())) {
            throw new MercadoPagoException("Solo se pueden cancelar preferencias pendientes");
        }
        
        preference.setStatus("cancelled");
        paymentPreferenceRepository.save(preference);
    }

    public Map<String, Object> getPaymentStatistics(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
        
        List<PaymentPreference> allPreferences = paymentPreferenceRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        Map<String, Object> statistics = new HashMap<>();
        
        // Contadores por estado
        long pendingCount = allPreferences.stream().filter(p -> "pending".equals(p.getStatus())).count();
        long approvedCount = allPreferences.stream().filter(p -> "approved".equals(p.getStatus())).count();
        long cancelledCount = allPreferences.stream().filter(p -> "cancelled".equals(p.getStatus())).count();
        long rejectedCount = allPreferences.stream().filter(p -> "rejected".equals(p.getStatus())).count();
        
        // Total de ingresos (solo pagos aprobados)
        double totalEarnings = allPreferences.stream()
                .filter(p -> "approved".equals(p.getStatus()))
                .mapToDouble(PaymentPreference::getAmount)
                .sum();
        
        // Preferencias del último mes
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        long recentCount = allPreferences.stream()
                .filter(p -> p.getCreatedAt().isAfter(oneMonthAgo))
                .count();
        
        statistics.put("totalPreferences", allPreferences.size());
        statistics.put("pendingCount", pendingCount);
        statistics.put("approvedCount", approvedCount);
        statistics.put("cancelledCount", cancelledCount);
        statistics.put("rejectedCount", rejectedCount);
        statistics.put("totalEarnings", totalEarnings);
        statistics.put("recentCount", recentCount);
        
        return statistics;
    }
}

