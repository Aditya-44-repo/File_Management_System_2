package com.fileload.service;

import com.fileload.model.dto.admin.AdminAnalyticsDTO;
import com.fileload.model.dto.admin.AdminAuditEventDTO;
import com.fileload.model.dto.admin.AdminUserSummaryDTO;
import com.fileload.model.entity.UserRole;
import org.springframework.data.domain.Page;

public interface AdminService {

    Page<AdminUserSummaryDTO> listUsers(String query, int page, int size);

    AdminUserSummaryDTO updateUserRole(Long userId, UserRole role);

    AdminUserSummaryDTO updateUserEnabled(Long userId, boolean enabled);

    AdminUserSummaryDTO resetFailedLoginAttempts(Long userId);

    AdminUserSummaryDTO forceLogout(Long userId);

    AdminAnalyticsDTO getAnalytics();

    Page<AdminAuditEventDTO> getAuditEvents(int page, int size);

    String exportAuditEventsAsCsv();

    void audit(String action, String targetType, String targetId, String details);
}

