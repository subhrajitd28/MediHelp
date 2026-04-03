package com.medihelp.prescription.service;

import com.medihelp.prescription.dto.MedicationScheduleResponse;
import com.medihelp.prescription.entity.MedicationSchedule;
import com.medihelp.prescription.repository.MedicationScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationScheduleService {

    private final MedicationScheduleRepository scheduleRepository;

    public List<MedicationScheduleResponse> getScheduleForToday(UUID userId) {
        log.info("Fetching today's medication schedule for user: {}", userId);

        String todayDayOfWeek = LocalDate.now().getDayOfWeek().name().substring(0, 3);

        List<MedicationSchedule> allActive = scheduleRepository.findByUserIdAndIsActiveTrue(userId);

        return allActive.stream()
                .filter(s -> s.getMedication().getIsActive())
                .filter(s -> s.getDayOfWeek() == null || s.getDayOfWeek().equalsIgnoreCase(todayDayOfWeek))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<MedicationScheduleResponse> getSchedules(UUID userId) {
        log.info("Fetching all schedules for user: {}", userId);
        return scheduleRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private MedicationScheduleResponse mapToResponse(MedicationSchedule schedule) {
        return MedicationScheduleResponse.builder()
                .id(schedule.getId())
                .medicationId(schedule.getMedication().getId())
                .drugName(schedule.getMedication().getDrugName())
                .dosage(schedule.getMedication().getDosage())
                .withFood(schedule.getMedication().getWithFood())
                .scheduledTime(schedule.getScheduledTime())
                .dayOfWeek(schedule.getDayOfWeek())
                .build();
    }
}
