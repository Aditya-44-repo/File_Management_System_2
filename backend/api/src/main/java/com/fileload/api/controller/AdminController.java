package com.fileload.api.controller;

import com.fileload.api.security.SecurityControlService;
import com.fileload.model.dto.FileLoadResponseDTO;
import com.fileload.model.dto.UpdateStatusRequestDTO;
import com.fileload.model.dto.admin.AdminAnalyticsDTO;
import com.fileload.model.dto.admin.AdminAuditEventDTO;
import com.fileload.model.dto.admin.AdminSimpleResultDTO;
import com.fileload.model.dto.admin.AdminUserSummaryDTO;
import com.fileload.model.dto.admin.BlockedIpRequestDTO;
import com.fileload.model.dto.admin.FeatureFlagUpdateRequestDTO;
import com.fileload.model.dto.admin.UserFileCountDTO;
import com.fileload.model.dto.admin.UserEnabledUpdateRequestDTO;
import com.fileload.model.dto.admin.UserRoleUpdateRequestDTO;
import com.fileload.service.AdminService;
import com.fileload.service.FileLoadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin")
public class AdminController {

    private final AdminService adminService;
    private final FileLoadService fileLoadService;
    private final SecurityControlService securityControlService;

    public AdminController(AdminService adminService,
                           FileLoadService fileLoadService,
                           SecurityControlService securityControlService) {
        this.adminService = adminService;
        this.fileLoadService = fileLoadService;
        this.securityControlService = securityControlService;
    }


    @GetMapping("/users")
    @PreAuthorize("@adminAuthorization.has(T(com.fileload.model.entity.AdminPermission).USER_ACCESS_CONTROL) || @adminAuthorization.has(T(com.fileload.model.entity.AdminPermission).USER_RECORDS_OVERVIEW) || @adminAuthorization.has(T(com.fileload.model.entity.AdminPermission).USER_FILES_DELETE_ALL)")
    @Operation(summary = "List users with admin visibility")
    public ResponseEntity<Page<AdminUserSummaryDTO>> listUsers(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.listUsers(query, page, size));
    }

    @PatchMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Change user role")
    public ResponseEntity<AdminUserSummaryDTO> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(adminService.updateUserRole(userId, request.role()));
    }

    @PatchMapping("/users/{userId}/enabled")
    @PreAuthorize("@adminAuthorization.has(T(com.fileload.model.entity.AdminPermission).USER_ACCESS_CONTROL)")
    @Operation(summary = "Enable or disable user")
    public ResponseEntity<AdminUserSummaryDTO> updateUserEnabled(
            @PathVariable Long userId,
            @RequestBody UserEnabledUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(adminService.updateUserEnabled(userId, request.enabled()));
    }

    @PostMapping("/users/{userId}/reset-failed-attempts")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Reset user failed login attempts")
    public ResponseEntity<AdminUserSummaryDTO> resetFailedAttempts(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.resetFailedLoginAttempts(userId));
    }

    @PostMapping("/users/{userId}/force-logout")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Force logout user by revoking active JWTs")
    public ResponseEntity<AdminUserSummaryDTO> forceLogout(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.forceLogout(userId));
    }

    @GetMapping("/users/{userId}/file-count")
    @PreAuthorize("@adminAuthorization.has(T(com.fileload.model.entity.AdminPermission).USER_RECORDS_OVERVIEW)")
    @Operation(summary = "Get total uploaded file count for a user")
    public ResponseEntity<UserFileCountDTO> getUserFileCount(@PathVariable Long userId) {
        long count = fileLoadService.countFilesByUserId(userId);
        return ResponseEntity.ok(new UserFileCountDTO(userId, count));
    }

    @DeleteMapping("/users/{userId}/files")
    @PreAuthorize("@adminAuthorization.has(T(com.fileload.model.entity.AdminPermission).USER_FILES_DELETE_ALL)")
    @Operation(summary = "Delete all files uploaded by a user")
    public ResponseEntity<AdminSimpleResultDTO> deleteAllUserFiles(@PathVariable Long userId) {
        long deletedCount = fileLoadService.deleteAllFilesByUserId(userId);
        adminService.audit("USER_FILES_DELETED_ALL", "USER", userId.toString(), "deletedCount=" + deletedCount);
        return ResponseEntity.ok(new AdminSimpleResultDTO("Deleted files count: " + deletedCount));
    }

    @PutMapping("/files/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Approve or reject upload by status update")
    public ResponseEntity<FileLoadResponseDTO> updateFileStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequestDTO request
    ) {
        FileLoadResponseDTO response = fileLoadService.updateFileLoadStatus(id, request.status(), request.comment());
        adminService.audit("FILE_STATUS_UPDATED", "FILE", id.toString(), "status=" + request.status());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/files/{id}/reprocess")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Reprocess failed upload")
    public ResponseEntity<FileLoadResponseDTO> reprocessFailed(@PathVariable Long id) {
        FileLoadResponseDTO response = fileLoadService.retryFileLoad(id);
        adminService.audit("FILE_REPROCESS_TRIGGERED", "FILE", id.toString(), "status=PENDING");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/files/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete any file")
    public ResponseEntity<AdminSimpleResultDTO> deleteFile(@PathVariable Long id) {
        fileLoadService.deleteFileLoad(id);
        adminService.audit("FILE_DELETED", "FILE", id.toString(), "deleted=true");
        return ResponseEntity.status(HttpStatus.OK).body(new AdminSimpleResultDTO("File deleted"));
    }

    @PostMapping("/security/blocked-ips")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Block suspicious IP")
    public ResponseEntity<AdminSimpleResultDTO> blockIp(@Valid @RequestBody BlockedIpRequestDTO request) {
        securityControlService.blockIp(request.ipAddress());
        adminService.audit("IP_BLOCKED", "IP", request.ipAddress(), "blocked=true");
        return ResponseEntity.ok(new AdminSimpleResultDTO("IP blocked"));
    }

    @DeleteMapping("/security/blocked-ips")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Unblock IP")
    public ResponseEntity<AdminSimpleResultDTO> unblockIp(@Valid @RequestBody BlockedIpRequestDTO request) {
        securityControlService.unblockIp(request.ipAddress());
        adminService.audit("IP_UNBLOCKED", "IP", request.ipAddress(), "blocked=false");
        return ResponseEntity.ok(new AdminSimpleResultDTO("IP unblocked"));
    }

    @GetMapping("/security/blocked-ips")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List blocked IPs")
    public ResponseEntity<Set<String>> getBlockedIps() {
        return ResponseEntity.ok(securityControlService.getBlockedIps());
    }

    @PutMapping("/feature-flags/{flagKey}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Set feature flag state")
    public ResponseEntity<AdminSimpleResultDTO> setFeatureFlag(
            @PathVariable String flagKey,
            @RequestBody FeatureFlagUpdateRequestDTO request
    ) {
        securityControlService.setFeatureFlag(flagKey, request.enabled());
        adminService.audit("FEATURE_FLAG_UPDATED", "FEATURE_FLAG", flagKey, "enabled=" + request.enabled());
        return ResponseEntity.ok(new AdminSimpleResultDTO("Feature flag updated"));
    }

    @GetMapping("/feature-flags")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List feature flags")
    public ResponseEntity<Map<String, Boolean>> getFeatureFlags() {
        return ResponseEntity.ok(securityControlService.getFeatureFlags());
    }

    @GetMapping("/analytics")
    @PreAuthorize("@adminAuthorization.has(T(com.fileload.model.entity.AdminPermission).USER_RECORDS_OVERVIEW)")
    @Operation(summary = "Get global system analytics")
    public ResponseEntity<AdminAnalyticsDTO> getAnalytics() {
        return ResponseEntity.ok(adminService.getAnalytics());
    }

    @GetMapping("/audit-events")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "View admin action logs")
    public ResponseEntity<Page<AdminAuditEventDTO>> getAuditEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(adminService.getAuditEvents(page, size));
    }

    @GetMapping("/audit-events/export")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Export admin action logs as CSV")
    public ResponseEntity<byte[]> exportAuditEvents() {
        String csv = adminService.exportAuditEventsAsCsv();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("admin-audit-events.csv").build());
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }
}




