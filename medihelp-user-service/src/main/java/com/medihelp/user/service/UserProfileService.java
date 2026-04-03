package com.medihelp.user.service;

import com.medihelp.common.exception.ResourceNotFoundException;
import com.medihelp.user.dto.UserProfileRequest;
import com.medihelp.user.dto.UserProfileResponse;
import com.medihelp.user.entity.UserProfile;
import com.medihelp.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository profileRepository;

    public UserProfileResponse getProfile(UUID userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "userId", userId));
        return toResponse(profile);
    }

    @Transactional
    public UserProfileResponse createProfile(UUID userId, String firstName, String email) {
        if (profileRepository.existsByUserId(userId)) {
            return getProfile(userId);
        }
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .firstName(firstName)
                .build();
        profile = profileRepository.save(profile);
        log.info("Created default profile for user {}", userId);
        return toResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UserProfileRequest request) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "userId", userId));

        if (request.getFirstName() != null) profile.setFirstName(request.getFirstName());
        if (request.getLastName() != null) profile.setLastName(request.getLastName());
        if (request.getDateOfBirth() != null) profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getBloodType() != null) profile.setBloodType(request.getBloodType());
        if (request.getHeight() != null) profile.setHeight(request.getHeight());
        if (request.getWeight() != null) profile.setWeight(request.getWeight());
        if (request.getProfilePictureUrl() != null) profile.setProfilePictureUrl(request.getProfilePictureUrl());
        if (request.getBio() != null) profile.setBio(request.getBio());

        profile = profileRepository.save(profile);
        return toResponse(profile);
    }

    private UserProfileResponse toResponse(UserProfile p) {
        return UserProfileResponse.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .dateOfBirth(p.getDateOfBirth())
                .gender(p.getGender())
                .bloodType(p.getBloodType())
                .height(p.getHeight())
                .weight(p.getWeight())
                .profilePictureUrl(p.getProfilePictureUrl())
                .bio(p.getBio())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
