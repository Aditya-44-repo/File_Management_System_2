package com.fileload.service;

import com.fileload.model.dto.admin.AdminInviteRequestDTO;
import com.fileload.model.dto.admin.AdminInviteResponseDTO;
import com.fileload.model.dto.admin.AdminUserSummaryDTO;
import com.fileload.model.entity.AdminPermission;
import java.util.Set;

public interface SuperAdminService {

    AdminInviteResponseDTO inviteAdmin(AdminInviteRequestDTO request);

    void acceptInvite(String token);

    boolean isInviteValid(String token);

    AdminUserSummaryDTO updateAdminPermissions(Long userId, Set<AdminPermission> permissions);
}

