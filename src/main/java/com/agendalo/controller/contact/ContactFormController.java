package com.agendalo.controller.contact;

import com.agendalo.dto.contact.ContactFormRequestDto;
import com.agendalo.dto.response.ApiResponse;
import com.agendalo.services.contact.ContactFormService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ContactFormController {

    private final ContactFormService contactFormService;

    @PostMapping
    public ResponseEntity<ApiResponse> createContactForm(@Valid @RequestBody ContactFormRequestDto requestDto) {
        log.info("Recibida solicitud de creación de formulario de contacto para: {}", requestDto.getEmail());
        return contactFormService.createContactForm(requestDto);
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAllContactForms() {
        log.info("Recibida solicitud para obtener todos los formularios de contacto");
        return contactFormService.getAllContactForms();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getContactFormById(@PathVariable Long id) {
        log.info("Recibida solicitud para obtener formulario de contacto con ID: {}", id);
        return contactFormService.getContactFormById(id);
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse> getUnreadContactForms() {
        log.info("Recibida solicitud para obtener formularios de contacto no leídos");
        return contactFormService.getUnreadContactForms();
    }

    @PutMapping("/{id}/mark-read")
    public ResponseEntity<ApiResponse> markAsRead(@PathVariable Long id) {
        log.info("Recibida solicitud para marcar como leído el formulario de contacto con ID: {}", id);
        return contactFormService.markAsRead(id);
    }

    @PutMapping("/{id}/response")
    public ResponseEntity<ApiResponse> addResponse(@PathVariable Long id, @RequestBody String respuesta) {
        log.info("Recibida solicitud para agregar respuesta al formulario de contacto con ID: {}", id);
        return contactFormService.addResponse(id, respuesta);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse> getContactFormsByEmail(@PathVariable String email) {
        log.info("Recibida solicitud para obtener formularios de contacto para email: {}", email);
        return contactFormService.getContactFormsByEmail(email);
    }
}


