package com.medihelp.health.repository;

import com.medihelp.health.entity.HealthScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthScoreRepository extends JpaRepository<HealthScore, UUID> {
    Optional<HealthScore> findFirstByUserIdOrderByCalculatedAtDesc(UUID userId);
    Page<HealthScore> findByUserIdOrderByCalculatedAtDesc(UUID userId, Pageable pageable);
}
