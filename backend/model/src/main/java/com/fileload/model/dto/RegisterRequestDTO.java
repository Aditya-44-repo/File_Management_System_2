package com.fileload.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
        @NotBlank
        @Size(min = 2, max = 100)
        String username,

        @NotBlank
        @Email
        @Pattern(
                regexp = "(?i)^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.com$",
                message = "must be a valid .com email address"
        )
        String email,

        @NotBlank
        @Size(min = 6, max = 255)
        String password
) {
}
