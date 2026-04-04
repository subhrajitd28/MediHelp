package com.medihelp.user.repository;

import com.medihelp.user.entity.SosShareableLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SosShareableLinkRepository extends JpaRepository<SosShareableLink, UUID> {
    Optional<SosShareableLink> findByToken(String token);
}
