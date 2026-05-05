package com.fileload.model.dto.admin;

import com.fileload.model.entity.AdminPermission;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record AdminPermissionsUpdateRequestDTO(@NotEmpty Set<AdminPermission> permissions) {
}

