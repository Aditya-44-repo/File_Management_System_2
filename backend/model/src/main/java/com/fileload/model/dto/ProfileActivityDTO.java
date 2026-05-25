package com.fileload.model.dto;

import java.time.LocalDateTime;

public record ProfileActivityDTO(
        String icon,
        String text,
        LocalDateTime timestamp
) {
}
