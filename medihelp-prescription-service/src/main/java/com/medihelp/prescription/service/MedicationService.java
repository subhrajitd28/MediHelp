package com.medihelp.prescription.service;

import com.medihelp.common.exception.ResourceNotFoundException;
import com.medihelp.prescription.dto.MedicationRequest;
import com.medihelp.prescription.dto.MedicationResponse;
import com.medihelp.prescription.dto.MedicationScheduleResponse;
import com.medihelp.prescription.entity.Medication;
import com.medihelp.prescription.entity.MedicationSchedule;
import com.medihelp.prescription.entity.Prescription;
import com.medihelp.prescription.repository.MedicationRepository;
import com.medihelp.prescription.repository.MedicationScheduleRepository;
import com.medihelp.prescription.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final MedicationScheduleRepository scheduleRepository;
    private final PrescriptionRepository prescriptionRepository;

    @Transactional
    public MedicationResponse addMedication(UUID userId, MedicationRequest request) {
        Prescription prescription = null;
        if (request.getPrescriptionId() != null) {
            prescription = prescriptionRepository.findByIdAndUserId(request.getPrescriptionId(), userId)
                    .orElse(null);
        }

        Medication medication = Medication.builder()
                .userId(userId)
                .prescription(prescription)
                .drugName(request.getDrugName())
                .dosage(request.getDosage())
                .frequency(request.getFrequency())
                .duration(request.getDuration())
                .withFood(request.getWithFood() != null ? request.getWithFood() : false)
                .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now())
                .endDate(request.getEndDate())
                .notes(request.getNotes())
                .build();
        medication = medicationRepository.save(medication);

        // Auto-create schedules based on frequency
        List<MedicationSchedule> schedules = createSchedules(medication, userId);
        return toResponse(medication, schedules);
    }

    private List<MedicationSchedule> createSchedules(Medication medication, UUID userId) {
        List<LocalTime> times = switch (medication.getFrequency() != null ? medication.getFrequency() : "") {
            case "TWICE_DAILY" -> List.of(LocalTime.of(8, 0), LocalTime.of(20, 0));
            case "THRICE_DAILY" -> List.of(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0));
            case "AS_NEEDED" -> List.of();
            default -> List.of(LocalTime.of(8, 0)); // ONCE_DAILY default
        };

        List<MedicationSchedule> schedules = new ArrayList<>();
        for (LocalTime time : times) {
            MedicationSchedule schedule = MedicationSchedule.builder()
                    .medication(medication)
                    .userId(userId)
                    .scheduledTime(time)
                    .build();
            schedules.add(scheduleRepository.save(schedule));
        }
        return schedules;
    }

    public List<MedicationResponse> getActiveMedications(UUID userId) {
        return medicationRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(m -> toResponse(m, scheduleRepository.findByMedicationIdAndIsActiveTrue(m.getId())))
                .toList();
    }

    public List<MedicationResponse> getAllMedications(UUID userId) {
        return medicationRepository.findByUserId(userId).stream()
                .map(m -> toResponse(m, scheduleRepository.findByMedicationIdAndIsActiveTrue(m.getId())))
                .toList();
    }

    @Transactional
    public MedicationResponse updateMedication(UUID userId, UUID medicationId, MedicationRequest request) {
        Medication m = medicationRepository.findByIdAndUserId(medicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Medication", "id", medicationId));
        if (request.getDrugName() != null) m.setDrugName(request.getDrugName());
        if (request.getDosage() != null) m.setDosage(request.getDosage());
        if (request.getDuration() != null) m.setDuration(request.getDuration());
        if (request.getWithFood() != null) m.setWithFood(request.getWithFood());
        if (request.getEndDate() != null) m.setEndDate(request.getEndDate());
        if (request.getNotes() != null) m.setNotes(request.getNotes());
        m = medicationRepository.save(m);
        return toResponse(m, scheduleRepository.findByMedicationIdAndIsActiveTrue(m.getId()));
    }

    @Transactional
    public void deactivateMedication(UUID userId, UUID medicationId) {
        Medication m = medicationRepository.findByIdAndUserId(medicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Medication", "id", medicationId));
        m.setIsActive(false);
        medicationRepository.save(m);
        // Deactivate schedules
        scheduleRepository.findByMedicationIdAndIsActiveTrue(medicationId)
                .forEach(s -> { s.setIsActive(false); scheduleRepository.save(s); });
    }

    private MedicationResponse toResponse(Medication m, List<MedicationSchedule> schedules) {
        List<MedicationScheduleResponse> scheduleResponses = schedules.stream()
                .map(s -> MedicationScheduleResponse.builder()
                        .id(s.getId()).medicationId(m.getId()).drugName(m.getDrugName())
                        .dosage(m.getDosage()).scheduledTime(s.getScheduledTime())
                        .dayOfWeek(s.getDayOfWeek()).withFood(m.getWithFood()).build())
                .toList();

        return MedicationResponse.builder()
                .id(m.getId())
                .prescriptionId(m.getPrescription() != null ? m.getPrescription().getId() : null)
                .drugName(m.getDrugName()).dosage(m.getDosage()).frequency(m.getFrequency())
                .duration(m.getDuration()).withFood(m.getWithFood()).startDate(m.getStartDate())
                .endDate(m.getEndDate()).isActive(m.getIsActive()).notes(m.getNotes())
                .schedules(scheduleResponses).createdAt(m.getCreatedAt()).build();
    }
}
