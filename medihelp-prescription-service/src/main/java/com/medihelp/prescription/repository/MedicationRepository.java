package com.medihelp.prescription.repository;

import com.medihelp.prescription.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, UUID> {
    List<Medication> findByUserIdAndIsActiveTrue(UUID userId);
    List<Medication> findByUserId(UUID userId);
    List<Medication> findByPrescriptionId(UUID prescriptionId);
    Optional<Medication> findByIdAndUserId(UUID id, UUID userId);
}
