package com.fileload.model.dto;

import java.time.LocalDateTime;

public record LoginHistoryDTO(
        String device,
        String browser,
        String ip,
        LocalDateTime time,
        String status
) {
}
