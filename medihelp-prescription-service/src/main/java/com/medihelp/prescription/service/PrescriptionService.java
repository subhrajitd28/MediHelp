package com.medihelp.prescription.service;

import com.medihelp.common.exception.ResourceNotFoundException;
import com.medihelp.prescription.dto.PrescriptionRequest;
import com.medihelp.prescription.dto.PrescriptionResponse;
import com.medihelp.prescription.entity.Prescription;
import com.medihelp.prescription.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;

    @Transactional
    public PrescriptionResponse createPrescription(UUID userId, PrescriptionRequest request) {
        Prescription prescription = Prescription.builder()
                .userId(userId)
                .doctorName(request.getDoctorName())
                .hospital(request.getHospital())
                .prescribedDate(request.getPrescribedDate())
                .notes(request.getNotes())
                .ocrText(request.getOcrText())
                .imageUrl(request.getImageUrl())
                .build();
        return toResponse(prescriptionRepository.save(prescription));
    }

    public List<PrescriptionResponse> getPrescriptions(UUID userId) {
        return prescriptionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    public PrescriptionResponse getPrescription(UUID userId, UUID prescriptionId) {
        return prescriptionRepository.findByIdAndUserId(prescriptionId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", prescriptionId));
    }

    @Transactional
    public PrescriptionResponse updatePrescription(UUID userId, UUID prescriptionId, PrescriptionRequest request) {
        Prescription p = prescriptionRepository.findByIdAndUserId(prescriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", prescriptionId));
        if (request.getDoctorName() != null) p.setDoctorName(request.getDoctorName());
        if (request.getHospital() != null) p.setHospital(request.getHospital());
        if (request.getPrescribedDate() != null) p.setPrescribedDate(request.getPrescribedDate());
        if (request.getNotes() != null) p.setNotes(request.getNotes());
        return toResponse(prescriptionRepository.save(p));
    }

    @Transactional
    public PrescriptionResponse confirmOcr(UUID userId, UUID prescriptionId, String confirmedText) {
        Prescription p = prescriptionRepository.findByIdAndUserId(prescriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", prescriptionId));
        p.setOcrText(confirmedText);
        p.setOcrConfirmed(true);
        return toResponse(prescriptionRepository.save(p));
    }

    private PrescriptionResponse toResponse(Prescription p) {
        return PrescriptionResponse.builder()
                .id(p.getId()).doctorName(p.getDoctorName()).hospital(p.getHospital())
                .prescribedDate(p.getPrescribedDate()).notes(p.getNotes()).ocrText(p.getOcrText())
                .ocrConfirmed(p.getOcrConfirmed()).imageUrl(p.getImageUrl()).createdAt(p.getCreatedAt())
                .build();
    }
}
