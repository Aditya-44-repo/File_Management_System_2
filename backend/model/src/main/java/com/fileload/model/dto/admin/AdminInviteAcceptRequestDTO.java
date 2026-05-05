package com.fileload.model.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminInviteAcceptRequestDTO(@NotBlank String token) {
}

