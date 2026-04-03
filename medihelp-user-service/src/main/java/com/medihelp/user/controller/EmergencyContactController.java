package com.medihelp.user.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.user.dto.EmergencyContactRequest;
import com.medihelp.user.dto.EmergencyContactResponse;
import com.medihelp.user.service.EmergencyContactService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/emergency-contacts")
@RequiredArgsConstructor
@Tag(name = "Emergency Contacts", description = "Emergency contact management")
public class EmergencyContactController {

    private final EmergencyContactService contactService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmergencyContactResponse>>> getContacts(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(contactService.getContacts(UUID.fromString(userId))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmergencyContactResponse>> addContact(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody EmergencyContactRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Contact added", contactService.addContact(UUID.fromString(userId), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmergencyContactResponse>> updateContact(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id,
            @Valid @RequestBody EmergencyContactRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Contact updated", contactService.updateContact(UUID.fromString(userId), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteContact(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        contactService.deleteContact(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success("Contact deleted", null));
    }
}
