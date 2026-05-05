package com.fileload.api.config;

import com.fileload.dao.repository.UserAccountRepository;
import com.fileload.model.entity.UserAccount;
import com.fileload.model.entity.UserRole;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SuperAdminBootstrap implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminBootstrap.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminBootstrap(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Value("${app.super-admin.email:superadmin@filemanagement.local}")
    private String email;

    @Value("${app.super-admin.username:superadmin}")
    private String username;

    @Value("${app.super-admin.password:}")
    private String password;


    @Override
    public void run(ApplicationArguments args) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String normalizedUsername = username == null || username.isBlank() ? "superadmin" : username.trim();
        UserAccount existing = userAccountRepository.findByEmail(normalizedEmail)
                .or(() -> userAccountRepository.findByUsername(normalizedUsername))
                .orElse(null);
        String configuredPassword = password == null ? "" : password.trim();

        if (existing != null) {
            existing.setEmail(normalizedEmail);
            existing.setUsername(normalizedUsername);
            existing.setRole(UserRole.SUPER_ADMIN);
            existing.setEnabled(true);
            existing.setAdminPermissions("USER_ACCESS_CONTROL,USER_RECORDS_OVERVIEW,USER_FILES_DELETE_ALL");
            if (!configuredPassword.isBlank()) {
                existing.setPassword(passwordEncoder.encode(configuredPassword));
            }
            userAccountRepository.save(existing);
            logger.info("Super admin credentials synced in DB for: {}", normalizedEmail);
            return;
        }

        String rawPassword = configuredPassword.isBlank() ? generatePassword() : configuredPassword;
        UserAccount superAdmin = new UserAccount();
        superAdmin.setUsername(resolveAvailableUsername(normalizedUsername, normalizedEmail));
        superAdmin.setEmail(normalizedEmail);
        superAdmin.setPassword(passwordEncoder.encode(rawPassword));
        superAdmin.setRole(UserRole.SUPER_ADMIN);
        superAdmin.setEnabled(true);
        superAdmin.setAdminPermissions("USER_ACCESS_CONTROL,USER_RECORDS_OVERVIEW,USER_FILES_DELETE_ALL");
        userAccountRepository.save(superAdmin);

        logger.warn("Bootstrapped SUPER_ADMIN account => email: {}, password: {}", normalizedEmail, rawPassword);
    }

    private String resolveAvailableUsername(String preferredUsername, String normalizedEmail) {
        if (!userAccountRepository.existsByUsername(preferredUsername)) {
            return preferredUsername;
        }

        String emailPrefix = normalizedEmail.contains("@")
                ? normalizedEmail.substring(0, normalizedEmail.indexOf('@'))
                : "superadmin";

        if (!userAccountRepository.existsByUsername(emailPrefix)) {
            return emailPrefix;
        }

        int index = 1;
        while (true) {
            String candidate = emailPrefix + "_" + index;
            if (!userAccountRepository.existsByUsername(candidate)) {
                return candidate;
            }
            index++;
        }
    }

    private String generatePassword() {
        byte[] buffer = new byte[18];
        RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }
}



