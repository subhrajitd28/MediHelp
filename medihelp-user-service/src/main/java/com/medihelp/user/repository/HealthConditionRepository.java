package com.medihelp.user.repository;

import com.medihelp.user.entity.HealthCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HealthConditionRepository extends JpaRepository<HealthCondition, UUID> {
    List<HealthCondition> findByUserId(UUID userId);
    List<HealthCondition> findByUserIdAndStatus(UUID userId, String status);
}
