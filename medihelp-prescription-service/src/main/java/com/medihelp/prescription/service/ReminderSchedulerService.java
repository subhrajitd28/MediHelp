package com.medihelp.prescription.service;

import com.medihelp.common.event.AppointmentReminderEvent;
import com.medihelp.common.event.MedicationReminderEvent;
import com.medihelp.common.event.RabbitMQConfig;
import com.medihelp.prescription.entity.Appointment;
import com.medihelp.prescription.entity.MedicationSchedule;
import com.medihelp.prescription.repository.AppointmentRepository;
import com.medihelp.prescription.repository.MedicationScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderSchedulerService {

    private final MedicationScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 60000) // every minute
    public void checkMedicationReminders() {
        LocalTime now = LocalTime.now();
        LocalTime windowEnd = now.plusMinutes(5);

        // Get all active schedules and check if any fall within the window
        // Simplified: in production, use a more efficient query
        List<MedicationSchedule> allActive = scheduleRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .filter(s -> !s.getScheduledTime().isBefore(now) && s.getScheduledTime().isBefore(windowEnd))
                .toList();

        for (MedicationSchedule schedule : allActive) {
            try {
                MedicationReminderEvent event = MedicationReminderEvent.builder()
                        .userId(schedule.getUserId().toString())
                        .medicationId(schedule.getMedication().getId().toString())
                        .drugName(schedule.getMedication().getDrugName())
                        .dosage(schedule.getMedication().getDosage())
                        .scheduledTime(Instant.now())
                        .withFood(schedule.getMedication().getWithFood())
                        .build();
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.RK_MEDICATION_REMINDER, event);
                log.debug("Medication reminder sent for user {} - {}", schedule.getUserId(), schedule.getMedication().getDrugName());
            } catch (Exception e) {
                log.error("Failed to send medication reminder: {}", e.getMessage());
            }
        }
    }

    @Scheduled(fixedRate = 900000) // every 15 minutes
    @Transactional
    public void checkAppointmentReminders() {
        Instant now = Instant.now();
        Instant in24Hours = now.plus(24, ChronoUnit.HOURS);

        List<Appointment> upcoming = appointmentRepository.findByScheduledAtBetweenAndReminderSentFalse(now, in24Hours);

        for (Appointment appointment : upcoming) {
            try {
                AppointmentReminderEvent event = AppointmentReminderEvent.builder()
                        .userId(appointment.getUserId().toString())
                        .appointmentId(appointment.getId().toString())
                        .doctorName(appointment.getDoctorName())
                        .hospital(appointment.getHospital())
                        .purpose(appointment.getPurpose())
                        .scheduledAt(appointment.getScheduledAt())
                        .build();
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.RK_APPOINTMENT_REMINDER, event);
                appointment.setReminderSent(true);
                appointmentRepository.save(appointment);
                log.info("Appointment reminder sent for user {} - Dr. {}", appointment.getUserId(), appointment.getDoctorName());
            } catch (Exception e) {
                log.error("Failed to send appointment reminder: {}", e.getMessage());
            }
        }
    }
}
