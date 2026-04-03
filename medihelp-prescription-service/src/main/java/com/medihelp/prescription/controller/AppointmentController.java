package com.medihelp.prescription.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.prescription.dto.AppointmentRequest;
import com.medihelp.prescription.dto.AppointmentResponse;
import com.medihelp.prescription.service.AppointmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prescriptions/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Doctor appointment management")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Appointment created", appointmentService.createAppointment(UUID.fromString(userId), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> list(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointments(UUID.fromString(userId))));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> upcoming(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getUpcomingAppointments(UUID.fromString(userId))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> update(
            @RequestHeader("X-User-Id") String userId, @PathVariable UUID id,
            @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Updated", appointmentService.updateAppointment(UUID.fromString(userId), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @RequestHeader("X-User-Id") String userId, @PathVariable UUID id) {
        appointmentService.cancelAppointment(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled", null));
    }
}
