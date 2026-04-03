package com.medihelp.user.service;

import com.medihelp.common.exception.ResourceNotFoundException;
import com.medihelp.user.dto.AllergyRequest;
import com.medihelp.user.dto.AllergyResponse;
import com.medihelp.user.entity.Allergy;
import com.medihelp.user.repository.AllergyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AllergyService {

    private final AllergyRepository allergyRepository;

    public List<AllergyResponse> getAllergies(UUID userId) {
        return allergyRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AllergyResponse addAllergy(UUID userId, AllergyRequest request) {
        Allergy allergy = Allergy.builder()
                .userId(userId)
                .allergen(request.getAllergen())
                .severity(request.getSeverity())
                .notes(request.getNotes())
                .diagnosedDate(request.getDiagnosedDate())
                .build();
        return toResponse(allergyRepository.save(allergy));
    }

    @Transactional
    public AllergyResponse updateAllergy(UUID userId, UUID allergyId, AllergyRequest request) {
        Allergy allergy = allergyRepository.findById(allergyId)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Allergy", "id", allergyId));

        allergy.setAllergen(request.getAllergen());
        allergy.setSeverity(request.getSeverity());
        allergy.setNotes(request.getNotes());
        allergy.setDiagnosedDate(request.getDiagnosedDate());
        return toResponse(allergyRepository.save(allergy));
    }

    @Transactional
    public void deleteAllergy(UUID userId, UUID allergyId) {
        Allergy allergy = allergyRepository.findById(allergyId)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Allergy", "id", allergyId));
        allergyRepository.delete(allergy);
    }

    private AllergyResponse toResponse(Allergy a) {
        return AllergyResponse.builder()
                .id(a.getId())
                .allergen(a.getAllergen())
                .severity(a.getSeverity())
                .notes(a.getNotes())
                .diagnosedDate(a.getDiagnosedDate())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
