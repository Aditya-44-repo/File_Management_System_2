package com.fileload.model.dto.admin;

import com.fileload.model.entity.UserRole;
import java.time.LocalDateTime;

public record AdminUserSummaryDTO(
        Long id,
        String username,
        String email,
        UserRole role,
        boolean enabled,
        int failedLoginAttempts,
        LocalDateTime accountLockedUntil,
        int tokenVersion
) {
}

