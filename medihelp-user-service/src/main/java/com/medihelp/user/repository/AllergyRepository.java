package com.medihelp.user.repository;

import com.medihelp.user.entity.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AllergyRepository extends JpaRepository<Allergy, UUID> {
    List<Allergy> findByUserId(UUID userId);
}
