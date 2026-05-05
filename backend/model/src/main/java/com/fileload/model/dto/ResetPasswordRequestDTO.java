package com.fileload.model.dto;

public record ResetPasswordRequestDTO(String token, String newPassword) {
}
