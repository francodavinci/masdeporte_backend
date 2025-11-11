package com.agendalo.exceptions.handlers;

import com.agendalo.dto.user.UserResponse;
import com.agendalo.exceptions.CustomException;
import com.agendalo.exceptions.MercadoPagoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<UserResponse> handleCustomException(CustomException ex) {
        UserResponse response = new UserResponse();
        response.setStatusCode(ex.getStatus().value());
        response.setMessage(ex.getMessage());
        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<UserResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        UserResponse response = new UserResponse();
        response.setStatusCode(409);
        response.setMessage("El email ya está registrado");
        return ResponseEntity.status(409).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<UserResponse> handleBadCredentials(BadCredentialsException ex) {
        UserResponse response = new UserResponse();
        response.setStatusCode(401);
        response.setMessage("Credenciales inválidas");
        return ResponseEntity.status(401).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<UserResponse> handleGenericException(Exception ex) {
        UserResponse response = new UserResponse();
        response.setStatusCode(500);
        response.setMessage("Error interno del servidor: " + ex.getMessage());
        return ResponseEntity.status(500).body(response);
    }

    @ExceptionHandler(MercadoPagoException.class)
    public ResponseEntity<UserResponse> handleMercadoPagoException(MercadoPagoException ex) {
        UserResponse response = new UserResponse();
        response.setStatusCode(ex.getStatus().value());
        response.setMessage(ex.getMessage());
        return new ResponseEntity<>(response, ex.getStatus());
    }
}