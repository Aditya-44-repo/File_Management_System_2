package com.fileload.model.dto.admin;

import java.time.LocalDateTime;

public record AdminInviteResponseDTO(
        String email,
        String generatedPassword,
        String token,
        LocalDateTime expiresAt,
        String inviteLink
) {
}


