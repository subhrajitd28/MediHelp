package com.medihelp.prescription.repository;

import com.medihelp.prescription.entity.DrugInteractionCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DrugInteractionCacheRepository extends JpaRepository<DrugInteractionCache, UUID> {
    Optional<DrugInteractionCache> findByDrug1AndDrug2(String drug1, String drug2);
    List<DrugInteractionCache> findByDrug1OrDrug2(String drug1, String drug2);
}
