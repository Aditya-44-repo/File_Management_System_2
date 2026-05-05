package com.fileload.service.impl;

import com.fileload.dao.repository.AdminAccessInviteRepository;
import com.fileload.dao.repository.UserAccountRepository;
import com.fileload.model.dto.admin.AdminInviteRequestDTO;
import com.fileload.model.dto.admin.AdminInviteResponseDTO;
import com.fileload.model.dto.admin.AdminUserSummaryDTO;
import com.fileload.model.entity.AdminAccessInvite;
import com.fileload.model.entity.AdminPermission;
import com.fileload.model.entity.UserAccount;
import com.fileload.model.entity.UserRole;
import com.fileload.service.AdminService;
import com.fileload.service.SuperAdminService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.security.SecureRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuperAdminServiceImpl implements SuperAdminService {

    private static final int INVITE_EXPIRY_HOURS = 48;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AdminAccessInviteRepository adminAccessInviteRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender javaMailSender;
    private final AdminService adminService;

    @Value("${app.frontend-base-url:https://localhost:4200}")
    private String frontendBaseUrl;

    @Value("${mail.from:noreply@filemanagement.com}")
    private String mailFrom;

    public SuperAdminServiceImpl(AdminAccessInviteRepository adminAccessInviteRepository,
                                 UserAccountRepository userAccountRepository,
                                 PasswordEncoder passwordEncoder,
                                 JavaMailSender javaMailSender,
                                 AdminService adminService) {
        this.adminAccessInviteRepository = adminAccessInviteRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.javaMailSender = javaMailSender;
        this.adminService = adminService;
    }

    @Override
    @Transactional
    public AdminInviteResponseDTO inviteAdmin(AdminInviteRequestDTO request) {
        adminAccessInviteRepository.deleteByEmail(request.email());

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(INVITE_EXPIRY_HOURS);
        String inviteLink = buildInviteLink(token);
        String generatedPassword = generatePassword();

        AdminAccessInvite invite = new AdminAccessInvite();
        invite.setToken(token);
        invite.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        invite.setPasswordHash(passwordEncoder.encode(generatedPassword));
        invite.setPermissionsCsv(toCsv(request.permissions()));
        invite.setInvitedByEmail(resolveActorEmail());
        invite.setExpiresAt(expiresAt);
        invite.setAccepted(false);
        adminAccessInviteRepository.save(invite);

        sendInviteEmail(invite.getEmail(), inviteLink, generatedPassword);
        adminService.audit("ADMIN_INVITE_SENT", "USER", invite.getEmail(), "permissions=" + invite.getPermissionsCsv());

        return new AdminInviteResponseDTO(invite.getEmail(), generatedPassword, token, expiresAt, inviteLink);
    }

    @Override
    @Transactional
    public void acceptInvite(String token) {
        AdminAccessInvite invite = adminAccessInviteRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite token"));

        if (!invite.isValid()) {
            throw new IllegalArgumentException("Invite token expired or already used");
        }

        UserAccount user = userAccountRepository.findByEmail(invite.getEmail()).orElseGet(UserAccount::new);
        if (user.getId() == null) {
            String username = invite.getEmail().split("@")[0];
            user.setEmail(invite.getEmail());
            user.setUsername(username);
        }

        user.setPassword(invite.getPasswordHash());
        user.setRole(UserRole.ADMIN);
        user.setEnabled(true);
        user.setAdminPermissions(invite.getPermissionsCsv());
        user.setTokenVersion(user.getTokenVersion() + 1);
        userAccountRepository.save(user);

        invite.setAccepted(true);
        invite.setAcceptedAt(LocalDateTime.now());
        adminAccessInviteRepository.save(invite);

        adminService.audit("ADMIN_INVITE_ACCEPTED", "USER", invite.getEmail(), "role=ADMIN");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInviteValid(String token) {
        return adminAccessInviteRepository.findByToken(token)
                .map(AdminAccessInvite::isValid)
                .orElse(false);
    }

    @Override
    @Transactional
    public AdminUserSummaryDTO updateAdminPermissions(Long userId, Set<AdminPermission> permissions) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        if (user.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Permissions can only be updated for ADMIN users");
        }

        user.setAdminPermissions(toCsv(permissions));
        UserAccount saved = userAccountRepository.save(user);
        adminService.audit("ADMIN_PERMISSIONS_UPDATED", "USER", userId.toString(), "permissions=" + saved.getAdminPermissions());

        return new AdminUserSummaryDTO(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getRole(),
                saved.isEnabled(),
                saved.getFailedLoginAttempts(),
                saved.getAccountLockedUntil(),
                saved.getTokenVersion()
        );
    }

    private String buildInviteLink(String token) {
        String base = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
        return base + "/admin-invite?token=" + token;
    }

    private void sendInviteEmail(String to, String inviteLink, String generatedPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(to);
        message.setSubject("Admin access invitation");
        message.setText("You have been invited as admin.\n\n"
                + "Login credentials:\n"
                + "Email: " + to + "\n"
                + "Password: " + generatedPassword + "\n\n"
                + "Click this link to accept the invite:\n"
                + inviteLink + "\n\n"
                + "This link expires in " + INVITE_EXPIRY_HOURS + " hours.");
        try {
            javaMailSender.send(message);
        } catch (MailException ex) {
            // Keep invite creation successful even if SMTP transport is unavailable.
            adminService.audit("ADMIN_INVITE_MAIL_FAILED", "USER", to, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private String generatePassword() {
        byte[] buffer = new byte[18];
        SECURE_RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private String toCsv(Set<AdminPermission> permissions) {
        return permissions.stream()
                .map(AdminPermission::name)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private String resolveActorEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    public static Set<AdminPermission> parsePermissions(String permissionsCsv) {
        if (permissionsCsv == null || permissionsCsv.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(permissionsCsv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(SuperAdminServiceImpl::safePermissionValueOf)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static AdminPermission safePermissionValueOf(String value) {
        try {
            return AdminPermission.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}



