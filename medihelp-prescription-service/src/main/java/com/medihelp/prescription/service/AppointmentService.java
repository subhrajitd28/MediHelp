package com.medihelp.prescription.service;

import com.medihelp.common.exception.ResourceNotFoundException;
import com.medihelp.prescription.dto.AppointmentRequest;
import com.medihelp.prescription.dto.AppointmentResponse;
import com.medihelp.prescription.entity.Appointment;
import com.medihelp.prescription.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    @Transactional
    public AppointmentResponse createAppointment(UUID userId, AppointmentRequest request) {
        Appointment appointment = Appointment.builder()
                .userId(userId).doctorName(request.getDoctorName())
                .hospital(request.getHospital()).specialization(request.getSpecialization())
                .purpose(request.getPurpose()).scheduledAt(request.getScheduledAt())
                .notes(request.getNotes()).build();
        return toResponse(appointmentRepository.save(appointment));
    }

    public List<AppointmentResponse> getAppointments(UUID userId) {
        return appointmentRepository.findByUserIdOrderByScheduledAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    public List<AppointmentResponse> getUpcomingAppointments(UUID userId) {
        return appointmentRepository.findByUserIdAndStatus(userId, "UPCOMING").stream()
                .map(this::toResponse).toList();
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
                .notes(a.getNotes()).createdAt(a.getCreatedAt()).build();
    }
}
