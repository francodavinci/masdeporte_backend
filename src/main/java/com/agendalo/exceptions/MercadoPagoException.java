package com.agendalo.exceptions;

import org.springframework.http.HttpStatus;

public class MercadoPagoException extends CustomException {
    public MercadoPagoException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public MercadoPagoException(String message, HttpStatus status) {
        super(message, status);
    }
}