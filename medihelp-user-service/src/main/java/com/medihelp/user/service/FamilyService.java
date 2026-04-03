package com.medihelp.user.service;

import com.medihelp.common.exception.BadRequestException;
import com.medihelp.common.exception.ResourceNotFoundException;
import com.medihelp.user.dto.*;
import com.medihelp.user.entity.FamilyGroup;
import com.medihelp.user.entity.FamilyMember;
import com.medihelp.user.repository.FamilyGroupRepository;
import com.medihelp.user.repository.FamilyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FamilyService {

    private final FamilyGroupRepository groupRepository;
    private final FamilyMemberRepository memberRepository;

    @Transactional
    public FamilyGroupResponse createGroup(UUID userId, FamilyGroupRequest request) {
        FamilyGroup group = FamilyGroup.builder()
                .name(request.getName())
                .createdByUserId(userId)
                .build();
        group = groupRepository.save(group);

        // Add creator as OWNER
        FamilyMember owner = FamilyMember.builder()
                .familyGroup(group)
                .userId(userId)
                .role("OWNER")
                .build();
        memberRepository.save(owner);

        return toGroupResponse(group);
    }

    public List<FamilyGroupResponse> getMyGroups(UUID userId) {
        return groupRepository.findByCreatedByUserId(userId).stream()
                .map(this::toGroupResponse)
                .toList();
    }

    @Transactional
    public FamilyMemberResponse addMember(UUID userId, UUID groupId, FamilyMemberRequest request) {
        FamilyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyGroup", "id", groupId));

        if (!group.getCreatedByUserId().equals(userId)) {
            throw new BadRequestException("Only the group owner can add members");
        }

        if (memberRepository.existsByFamilyGroupIdAndUserId(groupId, request.getUserId())) {
            throw new BadRequestException("User is already a member of this group");
        }

        FamilyMember member = FamilyMember.builder()
                .familyGroup(group)
                .userId(request.getUserId())
                .role(request.getRole() != null ? request.getRole() : "VIEWER")
                .build();
        member = memberRepository.save(member);
        return toMemberResponse(member);
    }

    public List<FamilyMemberResponse> getMembers(UUID groupId) {
        return memberRepository.findByFamilyGroupId(groupId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public void removeMember(UUID userId, UUID groupId, UUID memberId) {
        FamilyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyGroup", "id", groupId));

        if (!group.getCreatedByUserId().equals(userId)) {
            throw new BadRequestException("Only the group owner can remove members");
        }

        FamilyMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyMember", "id", memberId));
        memberRepository.delete(member);
    }

    private FamilyGroupResponse toGroupResponse(FamilyGroup g) {
        return FamilyGroupResponse.builder()
                .id(g.getId())
                .name(g.getName())
                .createdByUserId(g.getCreatedByUserId())
                .createdAt(g.getCreatedAt())
                .build();
    }

    private FamilyMemberResponse toMemberResponse(FamilyMember m) {
        return FamilyMemberResponse.builder()
                .id(m.getId())
                .familyGroupId(m.getFamilyGroup().getId())
                .userId(m.getUserId())
                .role(m.getRole())
                .addedAt(m.getAddedAt())
                .build();
    }
}
