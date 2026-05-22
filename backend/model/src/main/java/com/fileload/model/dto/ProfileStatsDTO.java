package com.fileload.model.dto;

public record ProfileStatsDTO(
        long totalUploads,
        long totalDownloads,
        long successfulUploads,
        long failedUploads
) {
}
