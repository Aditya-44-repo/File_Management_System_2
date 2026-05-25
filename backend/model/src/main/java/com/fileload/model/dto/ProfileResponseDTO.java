package com.fileload.model.dto;

import java.util.List;

public record ProfileResponseDTO(
        ProfileUserDTO user,
        ProfileStatsDTO stats,
        List<ProfileActivityDTO> recentActivities,
        List<LoginHistoryDTO> loginHistory,
        EmailChangeRequestDTO emailChangeRequest
) {
}
