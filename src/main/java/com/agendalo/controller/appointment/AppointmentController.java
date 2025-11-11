package com.agendalo.controller.appointment;

import com.agendalo.dto.appointment.CreateAppointmentRequest;
import com.agendalo.dto.appointment.UpdateAppointmentStatusRequest;
import com.agendalo.dto.response.ApiResponse;
import com.agendalo.services.appointment.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<ApiResponse> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return appointmentService.createAppointment(request, userDetails.getUsername());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getAppointmentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return appointmentService.getAppointmentById(id, userDetails.getUsername());
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse> getAppointmentsByUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        return appointmentService.getAppointmentsByUser(userDetails.getUsername());
    }

    @GetMapping("/company")
    public ResponseEntity<ApiResponse> getAppointmentsByCompany(
            @AuthenticationPrincipal UserDetails userDetails) {
        return appointmentService.getAppointmentsByCompany(userDetails.getUsername());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse> updateAppointmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAppointmentStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return appointmentService.updateAppointmentStatus(id, request, userDetails.getUsername());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> cancelAppointment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return appointmentService.cancelAppointment(id, userDetails.getUsername());
    }

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse> getAvailability(
            @RequestParam Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserDetails userDetails) {
        return appointmentService.getAvailableSlots(serviceId, date, userDetails.getUsername());
    }
}