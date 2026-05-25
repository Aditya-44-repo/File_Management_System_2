package com.fileload.model.dto;

import java.time.LocalDateTime;

public record EmailChangeRequestDTO(
        Long id,
        Long userId,
        String username,
        String oldEmail,
        String newEmail,
        String status,
        LocalDateTime requestedAt,
        LocalDateTime reviewedAt,
        Long reviewedBy
) {
}
