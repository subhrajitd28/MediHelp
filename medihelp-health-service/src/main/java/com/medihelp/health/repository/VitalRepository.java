package com.medihelp.health.repository;

import com.medihelp.health.entity.Vital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VitalRepository extends JpaRepository<Vital, UUID> {
    Page<Vital> findByUserIdAndTypeOrderByRecordedAtDesc(UUID userId, String type, Pageable pageable);
    Page<Vital> findByUserIdOrderByRecordedAtDesc(UUID userId, Pageable pageable);
    List<Vital> findByUserIdAndRecordedAtBetween(UUID userId, Instant from, Instant to);
    List<Vital> findByUserIdAndTypeAndRecordedAtBetween(UUID userId, String type, Instant from, Instant to);
    Optional<Vital> findFirstByUserIdAndTypeOrderByRecordedAtDesc(UUID userId, String type);
}
