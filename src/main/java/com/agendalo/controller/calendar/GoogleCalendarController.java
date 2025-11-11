package com.agendalo.controller.calendar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/google-calendar")
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarController {

    private final com.agendalo.services.calendar.GoogleCalendarService googleCalendarService;

    @GetMapping("/auth-url/{companyId}")
    public ResponseEntity<Map<String, String>> getAuthUrl(@PathVariable Long companyId) {
        try {
            String authUrl = googleCalendarService.getAuthorizationUrl(companyId);
            return ResponseEntity.ok(Map.of("authUrl", authUrl));
        } catch (Exception e) {
            log.error("Error generando URL de autorización", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error configurando Google Calendar"));
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam String code,
            @RequestParam String state) {
        try {
            googleCalendarService.handleCallback(code, state);
            return ResponseEntity.ok("Google Calendar configurado exitosamente");
        } catch (Exception e) {
            log.error("Error en callback de Google", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error configurando Google Calendar");
        }
    }

    @GetMapping("/status/{companyId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long companyId) {
        try {
            boolean isConfigured = googleCalendarService.isConfigured(companyId);
            return ResponseEntity.ok(Map.of("configured", isConfigured));
        } catch (Exception e) {
            log.error("Error verificando estado de Google Calendar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error verificando configuración"));
        }
    }
}