package com.fileload.model.dto;

import java.time.LocalDateTime;

public record DashboardOverviewDTO(
        long totalUploads,
        long inProcessing,
        double successRate,
        long exceptionsToday,
        long pendingCount,
        long processingCount,
        long successCount,
        LocalDateTime lastUpdated
) {
}
