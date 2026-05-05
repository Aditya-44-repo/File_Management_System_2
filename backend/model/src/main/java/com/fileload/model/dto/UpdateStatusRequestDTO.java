package com.fileload.model.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateStatusRequestDTO(
        @NotBlank
        String status,
        String comment
) {
}
