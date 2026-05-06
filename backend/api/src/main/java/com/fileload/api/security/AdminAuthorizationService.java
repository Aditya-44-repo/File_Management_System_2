package com.fileload.api.security;

import com.fileload.dao.repository.UserAccountRepository;
import com.fileload.model.entity.AdminPermission;
import com.fileload.model.entity.UserAccount;
import com.fileload.model.entity.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("adminAuthorization")
public class AdminAuthorizationService {

    private final UserAccountRepository userAccountRepository;

    public AdminAuthorizationService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }


    public boolean has(AdminPermission permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return false;
        }

        UserAccount user = userAccountRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return false;
        }
        return user.getRole() == UserRole.ADMIN;
    }
}
