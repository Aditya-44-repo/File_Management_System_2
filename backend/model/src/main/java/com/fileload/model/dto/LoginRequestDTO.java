package com.fileload.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank
        @JsonAlias("email")
        String login,

        @NotBlank
        String password
) {
}
