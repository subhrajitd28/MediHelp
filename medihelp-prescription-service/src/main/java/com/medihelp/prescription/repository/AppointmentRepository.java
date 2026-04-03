package com.medihelp.prescription.repository;

import com.medihelp.prescription.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    List<Appointment> findByUserIdOrderByScheduledAtDesc(UUID userId);
    List<Appointment> findByUserIdAndStatus(UUID userId, String status);
    List<Appointment> findByScheduledAtBetweenAndReminderSentFalse(Instant from, Instant to);
    Optional<Appointment> findByIdAndUserId(UUID id, UUID userId);
}
