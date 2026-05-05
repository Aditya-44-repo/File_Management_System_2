package com.fileload.model.dto.admin;

public record AdminAnalyticsDTO(
        long totalUsers,
        long enabledUsers,
        long totalFiles,
        long pendingFiles,
        long processingFiles,
        long successFiles,
        long failedFiles,
        long totalStorageBytes
) {
}

