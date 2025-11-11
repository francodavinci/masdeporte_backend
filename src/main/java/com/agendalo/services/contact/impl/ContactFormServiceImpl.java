package com.agendalo.services.contact.impl;

import com.agendalo.domain.ContactForm;
import com.agendalo.dto.contact.ContactFormRequestDto;
import com.agendalo.dto.contact.ContactFormResponseDto;
import com.agendalo.dto.response.ApiResponse;
import com.agendalo.exceptions.ResourceNotFoundException;
import com.agendalo.repository.ContactFormRepository;
import com.agendalo.services.contact.ContactFormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactFormServiceImpl implements ContactFormService {

    private final ContactFormRepository contactFormRepository;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> createContactForm(ContactFormRequestDto requestDto) {
        try {
            log.info("Creando nuevo formulario de contacto para: {}", requestDto.getEmail());

            // Crear la entidad ContactForm
            ContactForm contactForm = new ContactForm();
            contactForm.setName(requestDto.getName());
            contactForm.setEmail(requestDto.getEmail());
            contactForm.setPhone(requestDto.getPhone());
            contactForm.setInquiry(requestDto.getInquiry());
            contactForm.setRead(false);

            // Guardar en la base de datos
            ContactForm savedContactForm = contactFormRepository.save(contactForm);

            log.info("Formulario de contacto creado exitosamente con ID: {}", savedContactForm.getId());

            // Convertir a DTO de respuesta
            ContactFormResponseDto responseDto = mapToResponseDto(savedContactForm);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Formulario de contacto enviado exitosamente", responseDto));

        } catch (Exception e) {
            log.error("Error al crear formulario de contacto", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al enviar el formulario de contacto: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getAllContactForms() {
        try {
            log.info("Obteniendo todos los formularios de contacto");

            List<ContactForm> contactForms = contactFormRepository.findAllOrderByCreatedAtDesc();

            if (contactForms.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse(true, "No hay formularios de contacto", List.of()));
            }

            List<ContactFormResponseDto> responseDtos = contactForms.stream()
                    .map(this::mapToResponseDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(true, 
                    "Formularios de contacto obtenidos exitosamente", responseDtos));

        } catch (Exception e) {
            log.error("Error al obtener formularios de contacto", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al obtener los formularios de contacto: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getContactFormById(Long id) {
        try {
            log.info("Obteniendo formulario de contacto con ID: {}", id);

            ContactForm contactForm = contactFormRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Formulario de contacto no encontrado"));

            ContactFormResponseDto responseDto = mapToResponseDto(contactForm);

            return ResponseEntity.ok(new ApiResponse(true, 
                    "Formulario de contacto obtenido exitosamente", responseDto));

        } catch (ResourceNotFoundException e) {
            log.warn("Formulario de contacto no encontrado con ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Error al obtener formulario de contacto con ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al obtener el formulario de contacto: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getUnreadContactForms() {
        try {
            log.info("Obteniendo formularios de contacto no leídos");

            List<ContactForm> unreadForms = contactFormRepository.findByRead(false);

            if (unreadForms.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse(true, "No hay formularios de contacto no leídos", List.of()));
            }

            List<ContactFormResponseDto> responseDtos = unreadForms.stream()
                    .map(this::mapToResponseDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(true, 
                    "Formularios de contacto no leídos obtenidos exitosamente", responseDtos));

        } catch (Exception e) {
            log.error("Error al obtener formularios de contacto no leídos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al obtener los formularios de contacto no leídos: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> markAsRead(Long id) {
        try {
            log.info("Marcando como leído el formulario de contacto con ID: {}", id);

            ContactForm contactForm = contactFormRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Formulario de contacto no encontrado"));

            contactForm.setRead(true);
            ContactForm updatedContactForm = contactFormRepository.save(contactForm);

            ContactFormResponseDto responseDto = mapToResponseDto(updatedContactForm);

            return ResponseEntity.ok(new ApiResponse(true, 
                    "Formulario de contacto marcado como leído", responseDto));

        } catch (ResourceNotFoundException e) {
            log.warn("Formulario de contacto no encontrado con ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Error al marcar como leído el formulario de contacto con ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al marcar como leído el formulario de contacto: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> addResponse(Long id, String respuesta) {
        try {
            log.info("Agregando respuesta al formulario de contacto con ID: {}", id);

            ContactForm contactForm = contactFormRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Formulario de contacto no encontrado"));

            contactForm.setResponse(respuesta);
            contactForm.setRespondedAt(LocalDateTime.now());
            contactForm.setRead(true);

            ContactForm updatedContactForm = contactFormRepository.save(contactForm);

            ContactFormResponseDto responseDto = mapToResponseDto(updatedContactForm);

            return ResponseEntity.ok(new ApiResponse(true, 
                    "Respuesta agregada exitosamente", responseDto));

        } catch (ResourceNotFoundException e) {
            log.warn("Formulario de contacto no encontrado con ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Error al agregar respuesta al formulario de contacto con ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al agregar respuesta: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getContactFormsByEmail(String email) {
        try {
            log.info("Obteniendo formularios de contacto para el email: {}", email);

            List<ContactForm> contactForms = contactFormRepository.findByEmail(email);

            if (contactForms.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse(true, 
                        "No se encontraron formularios de contacto para este email", List.of()));
            }

            List<ContactFormResponseDto> responseDtos = contactForms.stream()
                    .map(this::mapToResponseDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(true, 
                    "Formularios de contacto obtenidos exitosamente", responseDtos));

        } catch (Exception e) {
            log.error("Error al obtener formularios de contacto para email: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al obtener los formularios de contacto: " + e.getMessage()));
        }
    }

    private ContactFormResponseDto mapToResponseDto(ContactForm contactForm) {
        return ContactFormResponseDto.builder()
                .id(contactForm.getId())
                .name(contactForm.getName())
                .email(contactForm.getEmail())
                .phone(contactForm.getPhone())
                .inquiry(contactForm.getInquiry())
                .createdAt(contactForm.getCreatedAt())
                .read(contactForm.getRead())
                .response(contactForm.getResponse())
                .respondedAt(contactForm.getRespondedAt())
                .build();
    }
}
