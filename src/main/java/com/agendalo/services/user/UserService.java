package com.agendalo.services.user;

import com.agendalo.domain.User;
import com.agendalo.dto.user.UserRequest;
import com.agendalo.dto.user.UserResponse;
import com.agendalo.dto.user.UserProfileResponse;
import com.agendalo.exceptions.CustomException;
import com.agendalo.exceptions.DuplicateResourceException;
import com.agendalo.exceptions.ResourceNotFoundException;
import com.agendalo.repository.UserRepository;
import com.agendalo.services.auth.JWTDetailsService;
import com.agendalo.services.email.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JWTDetailsService jwtDetailsService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;


    public UserResponse getMyInfo(String email) {
        log.info("Obteniendo información del usuario con email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Intento de obtener información de usuario no existente: {}", email);
                    return new ResourceNotFoundException(
                            String.format("No se encontró usuario con email %s", email)
                    );
                });

        log.info("Información de usuario recuperada exitosamente: {}", email);
        return UserResponse.builder()
                .user(user)
                .statusCode(200)
                .message("Información de usuario recuperada exitosamente")
                .build();
    }

    public UserResponse refreshToken(UserRequest refreshTokenRequest) {
        log.info("Iniciando refresh de token");

        try {
            // Extraer email del token
            String email = jwtDetailsService.extractUsernameFromToken(refreshTokenRequest.getToken());
            log.debug("Email extraído del token: {}", email);

            // Buscar usuario
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.warn("Intento de refresh token para usuario no existente: {}", email);
                        return new ResourceNotFoundException("Usuario no encontrado");
                    });

            // Validar token
            if (!jwtDetailsService.isTokenValid(refreshTokenRequest.getToken(), user)) {
                log.warn("Intento de refresh con token inválido para usuario: {}", email);
                throw new CustomException("Token inválido o expirado", HttpStatus.UNAUTHORIZED);
            }

            // Generar nuevo token
            var jwt = jwtDetailsService.generateToken(user);
            log.info("Token refrescado exitosamente para usuario: {}", email);

            return UserResponse.builder()
                    .statusCode(200)
                    .token(jwt)
                    .refreshToken(refreshTokenRequest.getToken())
                    .expirationTime("24Hr")
                    .message("Token actualizado exitosamente")
                    .build();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al refrescar token: {}", e.getMessage());
            throw new CustomException("Error al refrescar token", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public UserResponse getAllUsers() {
        log.info("Obteniendo lista de todos los usuarios");

        try {
            List<User> users = userRepository.findAll();

            if (users.isEmpty()) {
                log.info("No se encontraron usuarios en el sistema");
                return UserResponse.builder()
                        .statusCode(204) // No Content
                        .message("No hay usuarios registrados")
                        .build();
            }

            log.info("Se encontraron {} usuarios", users.size());
            return UserResponse.builder()
                    .usersList(users)
                    .statusCode(200)
                    .message("Usuarios recuperados exitosamente")
                    .build();

        } catch (Exception e) {
            log.error("Error al obtener lista de usuarios: {}", e.getMessage());
            throw new CustomException(
                    "Error al obtener lista de usuarios",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    public UserResponse getUsersById(Long id) {
        log.info("Buscando usuario con ID: {}", id);

        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Intento de obtener usuario no existente ID: {}", id);
                        return new ResourceNotFoundException(
                                String.format("No se encontró usuario con ID %d", id)
                        );
                    });

            log.info("Usuario encontrado exitosamente ID: {}", id);
            return UserResponse.builder()
                    .user(user)
                    .statusCode(200)
                    .message(String.format("Usuario con ID %d encontrado exitosamente", id))
                    .build();

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al buscar usuario por ID {}: {}", id, e.getMessage());
            throw new CustomException(
                    "Error al buscar usuario",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    public UserResponse register(UserRequest registrationRequest) {
        log.info("Iniciando registro de usuario con email: {}", registrationRequest.getEmail());

        // Verificar email duplicado
        userRepository.findByEmail(registrationRequest.getEmail())
                .ifPresent(user -> {
                    log.warn("Intento de registro con email duplicado: {}", registrationRequest.getEmail());
                    throw new DuplicateResourceException(
                            String.format("El email %s ya está registrado", registrationRequest.getEmail())
                    );
                });

        try {
            User user = new User();
            user.setEmail(registrationRequest.getEmail());
            user.setRole(registrationRequest.getRole());
            user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
            user.setName(registrationRequest.getName());
            user.setRegistrationDate(LocalDateTime.now());
            User savedUser = userRepository.save(user);
            log.info("Usuario registrado exitosamente con ID: {}", savedUser.getId());

            // Enviar email de bienvenida (asíncrono - no bloquea el flujo principal)
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

            return UserResponse.builder()
                    .user(savedUser)
                    .message("Usuario registrado exitosamente")
                    .statusCode(200)
                    .build();

        } catch (Exception e) {
            log.error("Error al registrar usuario: {}", e.getMessage());
            throw new CustomException("Error al registrar usuario: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public UserResponse login(UserRequest loginRequest) {
        log.info("Iniciando proceso de login para: {}", loginRequest.getEmail());

        try {
            // Buscar usuario y manejar caso no encontrado
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> {
                        log.warn("Intento de login con email no registrado: {}", loginRequest.getEmail());
                        return new ResourceNotFoundException(
                                String.format("No existe usuario con el email %s", loginRequest.getEmail())
                        );
                    });

            // Autenticar
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Generar tokens
            var jwt = jwtDetailsService.generateToken(user);
            var refreshToken = jwtDetailsService.generateRefreshToken(new HashMap<>(), user);

            log.info("Login exitoso para usuario: {}", loginRequest.getEmail());

            return UserResponse.builder()
                    .statusCode(200)
                    .token(jwt)
                    .role(user.getRole())
                    .refreshToken(refreshToken)
                    .expirationTime("24Hrs")
                    .message("Inicio de sesión exitoso")
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("Intento de login fallido para: {}", loginRequest.getEmail());
            throw new CustomException("Email o contraseña incorrectos", HttpStatus.UNAUTHORIZED);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error en login: {}", e.getMessage());
            throw new CustomException("Error en el inicio de sesión", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public UserResponse updateUser(Long userId, UserRequest updatedUser) {
        log.info("Iniciando actualización de usuario ID: {}", userId);

        try {
            // Verificar si el usuario existe
            User existingUser = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.warn("Intento de actualización de usuario no existente ID: {}", userId);
                        return new ResourceNotFoundException(
                                String.format("No se encontró usuario con ID %d", userId)
                        );
                    });

            // Verificar si el nuevo email ya está en uso por otro usuario
            if (!existingUser.getEmail().equals(updatedUser.getEmail())) {
                userRepository.findByEmail(updatedUser.getEmail())
                        .ifPresent(user -> {
                            log.warn("Intento de actualización con email duplicado: {}", updatedUser.getEmail());
                            throw new DuplicateResourceException(
                                    String.format("El email %s ya está en uso", updatedUser.getEmail())
                            );
                        });
            }

            // Actualizar datos
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setRole(updatedUser.getRole());

            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }

            User savedUser = userRepository.save(existingUser);
            log.info("Usuario actualizado exitosamente ID: {}", userId);

            return UserResponse.builder()
                    .user(savedUser)
                    .statusCode(200)
                    .message("Usuario actualizado exitosamente")
                    .build();

        } catch (ResourceNotFoundException | DuplicateResourceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar usuario: {}", e.getMessage());
            throw new CustomException("Error al actualizar usuario", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public UserResponse deleteUser(Long userId) {
        log.info("Iniciando eliminación de usuario ID: {}", userId);

        // Verificar si el usuario existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Intento de eliminación de usuario no existente ID: {}", userId);
                    return new ResourceNotFoundException(
                            String.format("No se encontró usuario con ID %d", userId)
                    );
                });

        try {
            userRepository.delete(user);
            log.info("Usuario eliminado exitosamente ID: {}", userId);

            return UserResponse.builder()
                    .statusCode(200)
                    .message("Usuario eliminado exitosamente")
                    .build();

        } catch (Exception e) {
            log.error("Error al eliminar usuario: {}", e.getMessage());
            throw new CustomException("Error al eliminar usuario", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String email) {
        log.info("Obteniendo perfil del usuario con email: {}", email);

        try {
            // 1. Obtener información básica del usuario
            User user = userRepository.findByEmailWithDetails(email)
                    .orElseThrow(() -> {
                        log.warn("Intento de obtener perfil de usuario no existente: {}", email);
                        return new ResourceNotFoundException(
                                String.format("No se encontró usuario con email %s", email)
                        );
                    });

            // 2. Cargar las compañías
            User userWithCompanies = userRepository.findByIdWithCompanies(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

            // 3. Cargar las citas
            User userWithAppointments = userRepository.findByIdWithAppointments(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

            // 4. Combinar la información
            user.setCompanies(userWithCompanies.getCompanies());
            user.setAppointments(userWithAppointments.getAppointments());

            log.info("Perfil de usuario recuperado exitosamente: {}", email);
            return UserProfileResponse.fromEntity(user);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al obtener perfil de usuario: {}", e.getMessage());
            throw new CustomException(
                    "Error al obtener perfil de usuario",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
