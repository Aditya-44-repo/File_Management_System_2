package com.fileload.model.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FileLoadResponseDTO(
        Long id,
        String filename,
        String fileType,
        Long fileSize,
        String status,
        Long recordCount,
        String errors,
        LocalDateTime uploadDate,
        Long uploadedById,
        String uploadedBy,
        String description,
        List<String> tags
) {
}
