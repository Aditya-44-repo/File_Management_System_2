package com.fileload.model.dto.admin;

import com.fileload.model.entity.AdminPermission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record AdminInviteRequestDTO(
        @NotBlank @Email String email,
        @NotEmpty Set<AdminPermission> permissions
) {
}


