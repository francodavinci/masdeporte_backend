package com.agendalo.services.contact;

import com.agendalo.dto.contact.ContactFormRequestDto;
import com.agendalo.dto.contact.ContactFormResponseDto;
import com.agendalo.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ContactFormService {
    
    ResponseEntity<ApiResponse> createContactForm(ContactFormRequestDto requestDto);
    
    ResponseEntity<ApiResponse> getAllContactForms();
    
    ResponseEntity<ApiResponse> getContactFormById(Long id);
    
    ResponseEntity<ApiResponse> getUnreadContactForms();
    
    ResponseEntity<ApiResponse> markAsRead(Long id);
    
    ResponseEntity<ApiResponse> addResponse(Long id, String respuesta);
    
    ResponseEntity<ApiResponse> getContactFormsByEmail(String email);
}


