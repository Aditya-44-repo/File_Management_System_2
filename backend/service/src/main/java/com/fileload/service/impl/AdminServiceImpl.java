package com.fileload.service.impl;

import com.fileload.dao.repository.AdminAuditEventRepository;
import com.fileload.dao.repository.FileLoadRepository;
import com.fileload.dao.repository.UserAccountRepository;
import com.fileload.model.dto.admin.AdminAnalyticsDTO;
import com.fileload.model.dto.admin.AdminAuditEventDTO;
import com.fileload.model.dto.admin.AdminUserSummaryDTO;
import com.fileload.model.entity.AdminAuditEvent;
import com.fileload.model.entity.FileStatus;
import com.fileload.model.entity.UserAccount;
import com.fileload.model.entity.UserRole;
import com.fileload.model.entity.UserRole;
import com.fileload.service.AdminService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserAccountRepository userAccountRepository;
    private final FileLoadRepository fileLoadRepository;
    private final AdminAuditEventRepository adminAuditEventRepository;

    public AdminServiceImpl(UserAccountRepository userAccountRepository,
                            FileLoadRepository fileLoadRepository,
                            AdminAuditEventRepository adminAuditEventRepository) {
        this.userAccountRepository = userAccountRepository;
        this.fileLoadRepository = fileLoadRepository;
        this.adminAuditEventRepository = adminAuditEventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryDTO> listUsers(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        String safeQuery = query == null ? "" : query.trim();

        if (safeQuery.isEmpty()) {
            return userAccountRepository.findAll(pageable).map(this::toUserSummary);
        }

        return userAccountRepository
                .findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(safeQuery, safeQuery, pageable)
                .map(this::toUserSummary);
    }

    @Override
    @Transactional
    public AdminUserSummaryDTO updateUserRole(Long userId, UserRole role) {
        UserAccount actor = resolveActorUser();
        if (role == UserRole.SUPER_ADMIN && (actor == null || actor.getRole() != UserRole.SUPER_ADMIN)) {
            throw new org.springframework.security.access.AccessDeniedException("Only SUPER_ADMIN can assign SUPER_ADMIN role");
        }

        UserAccount user = getUserOrThrow(userId);
        if (user.getRole() == UserRole.SUPER_ADMIN && (actor == null || actor.getRole() != UserRole.SUPER_ADMIN)) {
            throw new org.springframework.security.access.AccessDeniedException("SUPER_ADMIN account can only be managed by SUPER_ADMIN");
        }

        user.setRole(role);
        UserAccount saved = userAccountRepository.save(user);
        audit("USER_ROLE_UPDATED", "USER", userId.toString(), "role=" + role.name());
        return toUserSummary(saved);
    }

    @Override
    @Transactional
    public AdminUserSummaryDTO updateUserEnabled(Long userId, boolean enabled) {
        UserAccount actor = resolveActorUser();
        UserAccount user = getUserOrThrow(userId);
        if (user.getRole() == UserRole.SUPER_ADMIN && (actor == null || actor.getRole() != UserRole.SUPER_ADMIN)) {
            throw new org.springframework.security.access.AccessDeniedException("SUPER_ADMIN can only be managed by SUPER_ADMIN");
        }
        user.setEnabled(enabled);
        user.setDisabledByRole(enabled ? null : resolveBlockingRole(actor));
        user.setTokenVersion(user.getTokenVersion() + 1);
        UserAccount saved = userAccountRepository.save(user);
        audit("USER_ENABLED_UPDATED", "USER", userId.toString(),
                "enabled=" + enabled + (enabled ? "" : ",blockedBy=" + saved.getDisabledByRole()));
        return toUserSummary(saved);
    }

    @Override
    @Transactional
    public AdminUserSummaryDTO resetFailedLoginAttempts(Long userId) {
        UserAccount actor = resolveActorUser();
        UserAccount user = getUserOrThrow(userId);
        if (user.getRole() == UserRole.SUPER_ADMIN && (actor == null || actor.getRole() != UserRole.SUPER_ADMIN)) {
            throw new org.springframework.security.access.AccessDeniedException("SUPER_ADMIN account can only be managed by SUPER_ADMIN");
        }
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        UserAccount saved = userAccountRepository.save(user);
        audit("USER_LOGIN_ATTEMPTS_RESET", "USER", userId.toString(), "failedAttempts=0");
        return toUserSummary(saved);
    }

    @Override
    @Transactional
    public AdminUserSummaryDTO forceLogout(Long userId) {
        UserAccount actor = resolveActorUser();
        UserAccount user = getUserOrThrow(userId);
        if (user.getRole() == UserRole.SUPER_ADMIN && (actor == null || actor.getRole() != UserRole.SUPER_ADMIN)) {
            throw new org.springframework.security.access.AccessDeniedException("SUPER_ADMIN account can only be managed by SUPER_ADMIN");
        }
        user.setTokenVersion(user.getTokenVersion() + 1);
        UserAccount saved = userAccountRepository.save(user);
        audit("USER_FORCE_LOGOUT", "USER", userId.toString(), "tokenVersion=" + saved.getTokenVersion());
        return toUserSummary(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAnalyticsDTO getAnalytics() {
        return new AdminAnalyticsDTO(
                userAccountRepository.count(),
                userAccountRepository.countByEnabledTrue(),
                fileLoadRepository.count(),
                fileLoadRepository.countByStatus(FileStatus.PENDING),
                fileLoadRepository.countByStatus(FileStatus.PROCESSING),
                fileLoadRepository.countByStatus(FileStatus.SUCCESS),
                fileLoadRepository.countByStatus(FileStatus.FAILED),
                fileLoadRepository.totalStorageBytes()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminAuditEventDTO> getAuditEvents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        return adminAuditEventRepository.findAll(pageable).map(this::toAuditDto);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportAuditEventsAsCsv() {
        List<AdminAuditEvent> events = adminAuditEventRepository.findAll(Sort.by(Sort.Direction.DESC, "occurredAt"));
        String header = "id,occurredAt,actorEmail,action,targetType,targetId,details";
        String body = events.stream()
                .map(this::toCsvLine)
                .collect(Collectors.joining("\n"));
        return body.isEmpty() ? header : header + "\n" + body;
    }

    @Override
    @Transactional
    public void audit(String action, String targetType, String targetId, String details) {
        AdminAuditEvent event = new AdminAuditEvent();
        event.setAction(action);
        event.setActorEmail(resolveActorEmail());
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setDetails(details);
        event.setOccurredAt(LocalDateTime.now());
        adminAuditEventRepository.save(event);
    }

    private AdminUserSummaryDTO toUserSummary(UserAccount user) {
        return new AdminUserSummaryDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getFailedLoginAttempts(),
                user.getAccountLockedUntil(),
                user.getTokenVersion()
        );
    }

    private AdminAuditEventDTO toAuditDto(AdminAuditEvent event) {
        return new AdminAuditEventDTO(
                event.getId(),
                event.getAction(),
                event.getActorEmail(),
                event.getTargetType(),
                event.getTargetId(),
                event.getDetails(),
                event.getOccurredAt()
        );
    }

    private String toCsvLine(AdminAuditEvent event) {
        return event.getId() + ","
                + event.getOccurredAt() + ","
                + csvSafe(event.getActorEmail()) + ","
                + csvSafe(event.getAction()) + ","
                + csvSafe(event.getTargetType()) + ","
                + csvSafe(event.getTargetId()) + ","
                + csvSafe(event.getDetails());
    }

    private String csvSafe(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }

    private UserAccount getUserOrThrow(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    private String resolveActorEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private UserAccount resolveActorUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return userAccountRepository.findByEmail(authentication.getName()).orElse(null);
    }

    private UserRole resolveBlockingRole(UserAccount actor) {
        if (actor == null) {
            return UserRole.ADMIN;
        }
        if (actor.getRole() == UserRole.SUPER_ADMIN) {
            return UserRole.SUPER_ADMIN;
        }
        return UserRole.ADMIN;
    }
}


