package com.medihelp.prescription.repository;

import com.medihelp.prescription.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
    List<Prescription> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Prescription> findByIdAndUserId(UUID id, UUID userId);
}
