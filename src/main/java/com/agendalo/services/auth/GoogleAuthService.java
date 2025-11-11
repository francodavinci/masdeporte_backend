package com.agendalo.services.auth;

import com.agendalo.domain.User;
import com.agendalo.dto.user.UserResponse;
import com.agendalo.repository.UserRepository;
import com.agendalo.services.email.EmailService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;

@Service
@Slf4j
public class GoogleAuthService {

    @Value("${google.clientId}")
    private String googleClientId;

    private final UserRepository userRepository;
    private final JWTDetailsService jwtDetailsService;
    private final EmailService emailService;

    public GoogleAuthService(UserRepository userRepository, JWTDetailsService jwtDetailsService, EmailService emailService) {
        this.userRepository = userRepository;
        this.jwtDetailsService = jwtDetailsService;
        this.emailService = emailService;
    }

    public UserResponse authenticateGoogle(String credential) {
        UserResponse response = new UserResponse();

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                response.setStatusCode(401);
                response.setMessage("Token de Google inválido");
                return response;
            }

            Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // Buscar o crear usuario
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setRole("USER");
                        newUser.setName(name);
                        newUser.setRegistrationDate(LocalDateTime.now());// Establecer el nombre desde Google
                        User savedUser = userRepository.save(newUser);
                        
                        // Enviar email de bienvenida solo para usuarios nuevos (asíncrono)
                        try {
                            emailService.sendWelcomeEmail(savedUser)
                                .exceptionally(throwable -> {
                                    log.error("Error asíncrono al enviar email de bienvenida al usuario: {}. Error: {}",
                                             savedUser.getEmail(), throwable.getMessage(), throwable);
                                    return null;
                                });
                            log.info("Email de bienvenida programado para envío asíncrono al usuario: {}", savedUser.getEmail());
                        } catch (Exception e) {
                            log.error("Error al programar envío de email de bienvenida al usuario: {}. Error: {}", 
                                     savedUser.getEmail(), e.getMessage(), e);
                            // No fallar el registro si falla el envío de email
                        }
                        
                        return savedUser;
                    });

            // Generar tokens JWT
            String jwt = jwtDetailsService.generateToken(user);
            String refreshToken = jwtDetailsService.generateRefreshToken(new HashMap<>(), user);

            response.setStatusCode(200);
            response.setToken(jwt);
            response.setRole(user.getRole());
            response.setRefreshToken(refreshToken);
            response.setExpirationTime("24Hrs");
            response.setMessage("Successfully Logged In with Google");
            response.setUser(user);

        } catch (Exception e) {
            response.setStatusCode(500);
            response.setMessage("Error en autenticación con Google: " + e.getMessage());
        }

        return response;
    }
}