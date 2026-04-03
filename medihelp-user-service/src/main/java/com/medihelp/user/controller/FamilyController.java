package com.medihelp.user.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.user.dto.*;
import com.medihelp.user.service.FamilyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/family")
@RequiredArgsConstructor
@Tag(name = "Family Hub", description = "Family group management")
public class FamilyController {

    private final FamilyService familyService;

    @PostMapping("/groups")
    public ResponseEntity<ApiResponse<FamilyGroupResponse>> createGroup(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody FamilyGroupRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Family group created", familyService.createGroup(UUID.fromString(userId), request)));
    }

    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<FamilyGroupResponse>>> getMyGroups(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(familyService.getMyGroups(UUID.fromString(userId))));
    }

    @PostMapping("/groups/{groupId}/members")
    public ResponseEntity<ApiResponse<FamilyMemberResponse>> addMember(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID groupId,
            @Valid @RequestBody FamilyMemberRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Member added", familyService.addMember(UUID.fromString(userId), groupId, request)));
    }

    @GetMapping("/groups/{groupId}/members")
    public ResponseEntity<ApiResponse<List<FamilyMemberResponse>>> getMembers(
            @PathVariable UUID groupId) {
        return ResponseEntity.ok(ApiResponse.success(familyService.getMembers(groupId)));
    }

    @DeleteMapping("/groups/{groupId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID groupId,
            @PathVariable UUID memberId) {
        familyService.removeMember(UUID.fromString(userId), groupId, memberId);
        return ResponseEntity.ok(ApiResponse.success("Member removed", null));
    }
}
