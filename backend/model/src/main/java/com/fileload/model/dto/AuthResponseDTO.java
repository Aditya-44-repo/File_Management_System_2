package com.fileload.model.dto;

public record AuthResponseDTO(
        Long id,
        String username,
        String email,
        String role,
        String token,
        String profileImage,
        String adminPermissions
) {
}
