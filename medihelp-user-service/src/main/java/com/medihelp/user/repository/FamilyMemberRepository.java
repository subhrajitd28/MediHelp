package com.medihelp.user.repository;

import com.medihelp.user.entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID> {
    List<FamilyMember> findByFamilyGroupId(UUID familyGroupId);
    List<FamilyMember> findByUserId(UUID userId);
    boolean existsByFamilyGroupIdAndUserId(UUID familyGroupId, UUID userId);
}
