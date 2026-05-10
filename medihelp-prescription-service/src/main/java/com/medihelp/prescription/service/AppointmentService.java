package com.medihelp.prescription.service;

import com.medihelp.common.exception.ResourceNotFoundException;
import com.medihelp.prescription.dto.AppointmentRequest;
import com.medihelp.prescription.dto.AppointmentResponse;
import com.medihelp.prescription.entity.Appointment;
import com.medihelp.prescription.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    // Grace window after the scheduled time before we flip UPCOMING → COMPLETED.
    // Most outpatient consults wrap up well within an hour; using 1h avoids
    // marking an in-progress appointment as completed prematurely.
    private static final Duration COMPLETION_GRACE = Duration.ofHours(1);

    @Transactional
    public AppointmentResponse createAppointment(UUID userId, AppointmentRequest request) {
        Appointment appointment = Appointment.builder()
                .userId(userId).doctorName(request.getDoctorName())
                .hospital(request.getHospital()).specialization(request.getSpecialization())
                .purpose(request.getPurpose()).scheduledAt(request.getScheduledAt())
                .notes(request.getNotes())
                .type(request.getType() != null ? request.getType() : "CONSULTATION")
                .build();
        return toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public List<AppointmentResponse> getAppointments(UUID userId) {
        markPastAsCompleted(userId);
        return appointmentRepository.findByUserIdOrderByScheduledAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public List<AppointmentResponse> getUpcomingAppointments(UUID userId) {
        markPastAsCompleted(userId);
        return appointmentRepository.findByUserIdAndStatus(userId, "UPCOMING").stream()
                .map(this::toResponse).toList();
    }

    /**
     * Lazy status transition — runs on every list call. Cheap (one indexed
     * scan over the user's UPCOMING rows) and avoids needing a scheduled job
     * for a college-scale demo. CANCELLED rows are untouched.
     */
    private void markPastAsCompleted(UUID userId) {
        Instant cutoff = Instant.now().minus(COMPLETION_GRACE);
        List<Appointment> stale = appointmentRepository.findByUserIdAndStatus(userId, "UPCOMING").stream()
                .filter(a -> a.getScheduledAt() != null && a.getScheduledAt().isBefore(cutoff))
                .toList();
        if (stale.isEmpty()) return;
        for (Appointment a : stale) a.setStatus("COMPLETED");
        appointmentRepository.saveAll(stale);
        log.info("Auto-completed {} past appointment(s) for user {}", stale.size(), userId);
    }

    @Transactional
    public AppointmentResponse updateAppointment(UUID userId, UUID appointmentId, AppointmentRequest request) {
        Appointment a = appointmentRepository.findByIdAndUserId(appointmentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));
        if (request.getDoctorName() != null) a.setDoctorName(request.getDoctorName());
        if (request.getHospital() != null) a.setHospital(request.getHospital());
        if (request.getSpecialization() != null) a.setSpecialization(request.getSpecialization());
        if (request.getPurpose() != null) a.setPurpose(request.getPurpose());
        if (request.getScheduledAt() != null) a.setScheduledAt(request.getScheduledAt());
        if (request.getNotes() != null) a.setNotes(request.getNotes());
        if (request.getType() != null) a.setType(request.getType());
        return toResponse(appointmentRepository.save(a));
    }

    @Transactional
    public void cancelAppointment(UUID userId, UUID appointmentId) {
        Appointment a = appointmentRepository.findByIdAndUserId(appointmentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));
        a.setStatus("CANCELLED");
        appointmentRepository.save(a);
    }

    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId()).doctorName(a.getDoctorName()).hospital(a.getHospital())
                .specialization(a.getSpecialization()).purpose(a.getPurpose())
                .scheduledAt(a.getScheduledAt()).status(a.getStatus())
                .notes(a.getNotes()).type(a.getType()).createdAt(a.getCreatedAt()).build();
    }
}
