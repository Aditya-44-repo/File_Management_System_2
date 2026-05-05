package com.fileload.api.security;

import com.fileload.dao.repository.UserAccountRepository;
import com.fileload.model.entity.UserAccount;
import com.fileload.model.entity.UserRole;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserAccountRepository userAccountRepository;

    public CustomUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        logger.info("Attempting to load user by login: {}", login);
        UserAccount account = userAccountRepository.findByEmailOrUsername(login, login)
                .orElseThrow(() -> {
                    logger.warn("User not found for login: {}", login);
                    return new UsernameNotFoundException("User not found");
                });

        if (!account.isEnabled()) {
            throw new DisabledException(blockedMessage(account));
        }

        return User.withUsername(account.getEmail())
                .password(account.getPassword())
                .authorities(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()))
                .disabled(false)
                .accountLocked(account.getAccountLockedUntil() != null && account.getAccountLockedUntil().isAfter(LocalDateTime.now()))
                .build();
    }

    private String blockedMessage(UserAccount account) {
        return account.getDisabledByRole() == UserRole.SUPER_ADMIN
                ? "You are blocked by superadmin"
                : "You are blocked by admin";
    }
}

