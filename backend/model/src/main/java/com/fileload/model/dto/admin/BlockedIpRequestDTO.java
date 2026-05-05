package com.fileload.model.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record BlockedIpRequestDTO(@NotBlank String ipAddress) {
}

