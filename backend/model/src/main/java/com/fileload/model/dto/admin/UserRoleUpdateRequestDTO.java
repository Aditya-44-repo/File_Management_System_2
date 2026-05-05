package com.fileload.model.dto.admin;

import com.fileload.model.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequestDTO(@NotNull UserRole role) {
}

