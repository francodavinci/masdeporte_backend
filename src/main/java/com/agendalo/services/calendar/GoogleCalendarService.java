package com.agendalo.services.calendar;

import com.agendalo.domain.Appointment;
import com.agendalo.domain.Company;
import com.agendalo.domain.GoogleCalendarToken;
import com.agendalo.repository.CompanyRepository;
import com.agendalo.repository.GoogleCalendarTokenRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarService {

    @Value("${google.calendar.client-id}")
    private String clientId;

    @Value("${google.calendar.client-secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect-uri}")
    private String redirectUri;

    @Value("${google.calendar.scopes}")
    private String scopesString;

    private final GoogleCalendarTokenRepository tokenRepository;
    private final CompanyRepository companyRepository;

    private static final String APPLICATION_NAME = "Agendalo";

    // Método para obtener los scopes desde la propiedad
    private List<String> getScopes() {
        return Arrays.asList(scopesString.split(","));
    }

    public String getAuthorizationUrl(Long companyId) {
        try {
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    clientId,
                    clientSecret,
                    getScopes()
            ).build();

            String state = "company_" + companyId;
            return flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .setState(state)
                    .build();
        } catch (Exception e) {
            log.error("Error generando URL de autorización", e);
            throw new RuntimeException("Error configurando Google Calendar", e);
        }
    }

    public void handleCallback(String code, String state) {
        try {
            Long companyId = Long.parseLong(state.replace("company_", ""));
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("Compañía no encontrada"));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    clientId,
                    clientSecret,
                    getScopes()
            ).build();

            GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri(redirectUri)
                    .execute();

            // Guardar o actualizar tokens
            GoogleCalendarToken existingToken = tokenRepository.findByCompanyId(companyId).orElse(null);
            GoogleCalendarToken token = existingToken != null ? existingToken : new GoogleCalendarToken();

            token.setCompany(company);
            token.setAccessToken(tokenResponse.getAccessToken());
            token.setRefreshToken(tokenResponse.getRefreshToken());
            token.setExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresInSeconds()));

            tokenRepository.save(token);
            log.info("Google Calendar configurado para empresa: {}", companyId);

        } catch (Exception e) {
            log.error("Error procesando callback de Google", e);
            throw new RuntimeException("Error configurando Google Calendar", e);
        }
    }

    public boolean isConfigured(Long companyId) {
        return tokenRepository.findByCompanyId(companyId).isPresent();
    }

    public void createAppointmentEvent(Appointment appointment) {
        try {
            GoogleCalendarToken token = tokenRepository.findByCompanyId(appointment.getCompany().getId())
                    .orElseThrow(() -> new RuntimeException("Google Calendar no configurado para esta empresa"));

            if (isTokenExpired(token)) {
                refreshToken(token);
            }

            Calendar calendar = getCalendarService(token);

            // Convertir LocalDateTime a DateTime de Google
            ZonedDateTime startZoned = appointment.getStartTime().atZone(ZoneId.of("America/Argentina/Buenos_Aires"));
            ZonedDateTime endZoned = appointment.getEndTime().atZone(ZoneId.of("America/Argentina/Buenos_Aires"));

            com.google.api.client.util.DateTime startDateTime = new com.google.api.client.util.DateTime(startZoned.toInstant().toEpochMilli());
            com.google.api.client.util.DateTime endDateTime = new com.google.api.client.util.DateTime(endZoned.toInstant().toEpochMilli());

            Event event = new Event()
                    .setSummary("Turno: " + appointment.getService().getName())
                    .setDescription(buildEventDescription(appointment))
                    .setStart(new EventDateTime()
                            .setDateTime(startDateTime)
                            .setTimeZone("America/Argentina/Buenos_Aires"))
                    .setEnd(new EventDateTime()
                            .setDateTime(endDateTime)
                            .setTimeZone("America/Argentina/Buenos_Aires"))
                    .setLocation(appointment.getCompany().getAddress())
                    .setReminders(new Event.Reminders()
                            .setUseDefault(false)
                            .setOverrides(Arrays.asList(
                                    new EventReminder().setMethod("popup").setMinutes(30),
                                    new EventReminder().setMethod("email").setMinutes(60)
                            )));
            Event createdEvent = calendar.events().insert("primary", event).execute();

            log.info("Evento creado en Google Calendar para turno: {}", appointment.getId());

        } catch (Exception e) {
            log.error("Error creando evento en Google Calendar", e);
            // No fallar la creación del turno si falla el calendario
        }
    }

    private String buildEventDescription(Appointment appointment) {
        return String.format(
                "Cliente: %s\n" +
                        "Servicio: %s\n" +
                        "Teléfono: %s\n" +
                        "Email: %s\n" +
                        "Notas: %s",
                appointment.getUser().getName(),
               appointment.getService().getName(),
               appointment.getUser().getPhoneNumber() != null ? appointment.getUser().getPhoneNumber()  : "No disponible",
               appointment.getUser().getEmail() != null ? appointment.getUser().getEmail() : "No disponible",
                appointment.getNotes() != null ? appointment.getNotes() : "Sin notas"
        );
    }

    private boolean isTokenExpired(GoogleCalendarToken token) {
        return token.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private void refreshToken(GoogleCalendarToken token) throws Exception {
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(token.getRefreshToken());

        credential.refreshToken();

        token.setAccessToken(credential.getAccessToken());
        token.setExpiresAt(LocalDateTime.now().plusSeconds(3600)); // 1 hora por defecto

        tokenRepository.save(token);
    }

    private Calendar getCalendarService(GoogleCalendarToken token) throws Exception {
        GoogleCredential credential = new GoogleCredential()
                .setAccessToken(token.getAccessToken());

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential
        ).setApplicationName(APPLICATION_NAME).build();
    }
}