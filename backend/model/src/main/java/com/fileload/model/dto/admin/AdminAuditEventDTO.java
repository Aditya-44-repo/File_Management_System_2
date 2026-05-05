package com.fileload.model.dto.admin;

import java.time.LocalDateTime;

public record AdminAuditEventDTO(
        Long id,
        String action,
        String actorEmail,
        String targetType,
        String targetId,
        String details,
        LocalDateTime occurredAt
) {
}

