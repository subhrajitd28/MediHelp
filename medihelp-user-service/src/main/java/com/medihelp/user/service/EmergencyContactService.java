package com.medihelp.user.service;

import com.medihelp.common.exception.ResourceNotFoundException;
import com.medihelp.user.dto.EmergencyContactRequest;
import com.medihelp.user.dto.EmergencyContactResponse;
import com.medihelp.user.entity.EmergencyContact;
import com.medihelp.user.repository.EmergencyContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmergencyContactService {

    private final EmergencyContactRepository contactRepository;

    public List<EmergencyContactResponse> getContacts(UUID userId) {
        return contactRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EmergencyContactResponse addContact(UUID userId, EmergencyContactRequest request) {
        EmergencyContact contact = EmergencyContact.builder()
                .userId(userId)
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .relationship(request.getRelationship())
                .isPrimary(request.isPrimary())
                .build();
        return toResponse(contactRepository.save(contact));
    }

    @Transactional
    public EmergencyContactResponse updateContact(UUID userId, UUID contactId, EmergencyContactRequest request) {
        EmergencyContact contact = contactRepository.findById(contactId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("EmergencyContact", "id", contactId));

        contact.setName(request.getName());
        contact.setPhone(request.getPhone());
        contact.setEmail(request.getEmail());
        contact.setRelationship(request.getRelationship());
        contact.setPrimary(request.isPrimary());
        return toResponse(contactRepository.save(contact));
    }

    @Transactional
    public void deleteContact(UUID userId, UUID contactId) {
        EmergencyContact contact = contactRepository.findById(contactId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("EmergencyContact", "id", contactId));
        contactRepository.delete(contact);
    }

    private EmergencyContactResponse toResponse(EmergencyContact c) {
        return EmergencyContactResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .relationship(c.getRelationship())
                .isPrimary(c.isPrimary())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
