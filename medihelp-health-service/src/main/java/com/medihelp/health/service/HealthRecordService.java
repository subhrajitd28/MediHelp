package com.medihelp.health.service;

import com.medihelp.common.exception.ResourceNotFoundException;
import com.medihelp.health.dto.HealthRecordRequest;
import com.medihelp.health.dto.HealthRecordResponse;
import com.medihelp.health.entity.HealthRecord;
import com.medihelp.health.repository.HealthRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HealthRecordService {

    private final HealthRecordRepository recordRepository;

    @Transactional
    public HealthRecordResponse createRecord(UUID userId, HealthRecordRequest request) {
        HealthRecord record = HealthRecord.builder()
                .userId(userId)
                .title(request.getTitle())
                .category(request.getCategory() != null ? request.getCategory() : "OTHER")
                .description(request.getDescription())
                .doctorName(request.getDoctorName())
                .hospital(request.getHospital())
                .recordDate(request.getRecordDate())
                .fileContentBase64(request.getFileContentBase64())
                .fileName(request.getFileName())
                .fileType(request.getFileType())
                .fileSize(request.getFileSize())
                .build();
        return toResponse(recordRepository.save(record));
    }

    public List<HealthRecordResponse> getRecords(UUID userId) {
        return recordRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    public List<HealthRecordResponse> getRecordsByCategory(UUID userId, String category) {
        return recordRepository.findByUserIdAndCategory(userId, category).stream()
                .map(this::toResponse).toList();
    }

    public String getFileContent(UUID userId, UUID recordId) {
        HealthRecord record = recordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("HealthRecord", "id", recordId));
        return record.getFileContentBase64();
    }

    @Transactional
    public void deleteRecord(UUID userId, UUID recordId) {
        HealthRecord record = recordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("HealthRecord", "id", recordId));
        recordRepository.delete(record);
    }

    private HealthRecordResponse toResponse(HealthRecord r) {
        return HealthRecordResponse.builder()
                .id(r.getId()).title(r.getTitle()).category(r.getCategory())
                .description(r.getDescription()).doctorName(r.getDoctorName())
                .hospital(r.getHospital()).recordDate(r.getRecordDate())
                .fileName(r.getFileName()).fileType(r.getFileType()).fileSize(r.getFileSize())
                .hasFile(r.getFileContentBase64() != null && !r.getFileContentBase64().isEmpty())
                .createdAt(r.getCreatedAt()).build();
    }
}
