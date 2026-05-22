package com.fileload.model.dto;

public record ProfileUserDTO(
        Long id,
        String username,
        String email,
        String role,
        String profileImage
) {
}
