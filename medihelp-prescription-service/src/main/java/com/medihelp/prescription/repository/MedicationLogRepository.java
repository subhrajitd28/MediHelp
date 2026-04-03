package com.medihelp.prescription.repository;

import com.medihelp.prescription.entity.MedicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationLogRepository extends JpaRepository<MedicationLog, UUID> {
    List<MedicationLog> findByUserIdAndScheduledTimeBetween(UUID userId, Instant from, Instant to);
    List<MedicationLog> findByMedicationIdOrderByScheduledTimeDesc(UUID medicationId);
    long countByUserIdAndStatusAndScheduledTimeBetween(UUID userId, String status, Instant from, Instant to);
}
