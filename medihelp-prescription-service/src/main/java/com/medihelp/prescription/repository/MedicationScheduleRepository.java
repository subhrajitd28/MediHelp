package com.medihelp.prescription.repository;

import com.medihelp.prescription.entity.MedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, UUID> {
    List<MedicationSchedule> findByMedicationIdAndIsActiveTrue(UUID medicationId);
    List<MedicationSchedule> findByUserIdAndIsActiveTrue(UUID userId);
    List<MedicationSchedule> findByUserId(UUID userId);
}
