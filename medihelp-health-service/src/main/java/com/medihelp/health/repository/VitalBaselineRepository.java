package com.medihelp.health.repository;

import com.medihelp.health.entity.VitalBaseline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VitalBaselineRepository extends JpaRepository<VitalBaseline, UUID> {
    Optional<VitalBaseline> findByUserIdAndVitalType(UUID userId, String vitalType);
}
