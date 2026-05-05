package com.fileload.api.controller;

import com.fileload.model.dto.admin.AdminInviteAcceptRequestDTO;
import com.fileload.model.dto.admin.AdminInviteRequestDTO;
import com.fileload.model.dto.admin.AdminInviteResponseDTO;
import com.fileload.model.dto.admin.AdminPermissionsUpdateRequestDTO;
import com.fileload.model.dto.admin.AdminSimpleResultDTO;
import com.fileload.model.dto.admin.AdminUserSummaryDTO;
import com.fileload.service.SuperAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/super-admin")
@Tag(name = "Super Admin")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }


    @PostMapping("/admin-invites")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Send admin invitation with scoped permissions")
    public ResponseEntity<AdminInviteResponseDTO> inviteAdmin(@Valid @RequestBody AdminInviteRequestDTO request) {
        return ResponseEntity.ok(superAdminService.inviteAdmin(request));
    }

    @PutMapping("/admins/{userId}/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update scoped permissions for an admin")
    public ResponseEntity<AdminUserSummaryDTO> updateAdminPermissions(
            @PathVariable Long userId,
            @Valid @RequestBody AdminPermissionsUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(superAdminService.updateAdminPermissions(userId, request.permissions()));
    }

    @GetMapping("/admin-invites/{token}/validate")
    @Operation(summary = "Validate admin invitation token")
    public ResponseEntity<Map<String, Boolean>> validateInvite(@PathVariable String token) {
        return ResponseEntity.ok(Map.of("valid", superAdminService.isInviteValid(token)));
    }

    @PostMapping("/admin-invites/accept")
    @Operation(summary = "Accept admin invitation")
    public ResponseEntity<AdminSimpleResultDTO> acceptInvite(@Valid @RequestBody AdminInviteAcceptRequestDTO request) {
        superAdminService.acceptInvite(request.token());
        return ResponseEntity.ok(new AdminSimpleResultDTO("Admin invite accepted. Login with provided credentials."));
    }
}

