package com.medihelp.user.service;

import com.medihelp.common.exception.ResourceNotFoundException;
import com.medihelp.user.dto.HealthConditionRequest;
import com.medihelp.user.dto.HealthConditionResponse;
import com.medihelp.user.entity.HealthCondition;
import com.medihelp.user.repository.HealthConditionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HealthConditionService {

    private final HealthConditionRepository conditionRepository;

    public List<HealthConditionResponse> getConditions(UUID userId) {
        return conditionRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HealthConditionResponse addCondition(UUID userId, HealthConditionRequest request) {
        HealthCondition condition = HealthCondition.builder()
                .userId(userId)
                .conditionName(request.getConditionName())
                .diagnosedDate(request.getDiagnosedDate())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .notes(request.getNotes())
                .build();
        return toResponse(conditionRepository.save(condition));
    }

    @Transactional
    public HealthConditionResponse updateCondition(UUID userId, UUID conditionId, HealthConditionRequest request) {
        HealthCondition condition = conditionRepository.findById(conditionId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("HealthCondition", "id", conditionId));

        condition.setConditionName(request.getConditionName());
        condition.setDiagnosedDate(request.getDiagnosedDate());
        if (request.getStatus() != null) condition.setStatus(request.getStatus());
        condition.setNotes(request.getNotes());
        return toResponse(conditionRepository.save(condition));
    }

    @Transactional
    public void deleteCondition(UUID userId, UUID conditionId) {
        HealthCondition condition = conditionRepository.findById(conditionId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("HealthCondition", "id", conditionId));
        conditionRepository.delete(condition);
    }

    private HealthConditionResponse toResponse(HealthCondition c) {
        return HealthConditionResponse.builder()
                .id(c.getId())
                .conditionName(c.getConditionName())
                .diagnosedDate(c.getDiagnosedDate())
                .status(c.getStatus())
                .notes(c.getNotes())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
