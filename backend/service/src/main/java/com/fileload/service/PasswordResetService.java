package com.fileload.service;

import com.fileload.dao.repository.PasswordResetTokenRepository;
import com.fileload.dao.repository.UserAccountRepository;
import com.fileload.model.entity.PasswordResetToken;
import com.fileload.model.entity.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserAccountRepository userAccountRepository;
    private final JavaMailSender javaMailSender;
    private final PasswordEncoder passwordEncoder;
    private final String frontendBaseUrl;
    private final String mailFrom;

    private static final int TOKEN_EXPIRY_HOURS = 24;

    public PasswordResetService(PasswordResetTokenRepository passwordResetTokenRepository,
                                UserAccountRepository userAccountRepository,
                                 JavaMailSender javaMailSender,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.frontend-base-url:https://localhost:4200}") String frontendBaseUrl,
                                @Value("${spring.mail.username:}") String mailSenderUsername,
                                @Value("${spring.mail.from:}") String mailFrom) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userAccountRepository = userAccountRepository;
        this.javaMailSender = javaMailSender;
        this.passwordEncoder = passwordEncoder;
        this.frontendBaseUrl = frontendBaseUrl;
        this.mailFrom = resolveMailFrom(mailSenderUsername, mailFrom);
    }

    private String resolveMailFrom(String mailSenderUsername, String mailFrom) {
        String trimmedSender = mailSenderUsername == null ? "" : mailSenderUsername.trim();
        String trimmedFrom = mailFrom == null ? "" : mailFrom.trim();

        if (trimmedSender.isBlank() && trimmedFrom.isBlank()) {
            logger.error("Missing mail sender configuration: MAIL_SMTP_USERNAME or MAIL_FROM must be set.");
            throw new IllegalStateException("Email sender is not configured. Set MAIL_SMTP_USERNAME and optionally MAIL_FROM.");
        }

        if (trimmedFrom.isBlank()) {
            logger.info("spring.mail.from is not configured, defaulting password reset sender to spring.mail.username {}", trimmedSender);
            return trimmedSender;
        }

        if (trimmedSender.isBlank()) {
            logger.warn("spring.mail.username is not configured. Using MAIL_FROM {} as sender, but SMTP auth may fail if username is missing.", trimmedFrom);
            return trimmedFrom;
        }

        if (!trimmedFrom.equalsIgnoreCase(trimmedSender)) {
            logger.warn("Configured MAIL_FROM {} does not match MAIL_SMTP_USERNAME {}. Using MAIL_SMTP_USERNAME for Gmail SMTP.", trimmedFrom, trimmedSender);
            return trimmedSender;
        }

        logger.info("Using resolved mail sender: {}", trimmedSender);
        return trimmedSender;
    }

    @Transactional
    public void requestPasswordReset(String email) {
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        // Delete any existing unused tokens for this user
        passwordResetTokenRepository.deleteByUserId(user.getId());

        // Generate new token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);

        PasswordResetToken resetToken = new PasswordResetToken(token, user.getId(), expiresAt);
        passwordResetTokenRepository.save(resetToken);

        // Send email
        sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    public boolean validateResetToken(String token) {
        Optional<PasswordResetToken> resetToken = passwordResetTokenRepository.findByToken(token);

        if (resetToken.isEmpty()) {
            return false;
        }

        PasswordResetToken tokenObj = resetToken.get();
        return tokenObj.isValid();
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        if (!resetToken.isValid()) {
            throw new IllegalStateException("Reset token has expired or already been used");
        }

        UserAccount user = userAccountRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userAccountRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
    }

    private void sendPasswordResetEmail(String email, String token) {
        try {
            String resetLink = frontendBaseUrl + "/reset-password?token=" + token;

            if (mailFrom == null || mailFrom.isBlank()) {
                logger.error("Password reset email sender is not configured. mailFrom is blank.");
                throw new IllegalStateException("Password reset email sender is not configured");
            }

            logger.info("Resolved password reset sender: {}", mailFrom);
            logger.info("Sending password reset email from={} to={}, frontend URL={}", mailFrom, email, frontendBaseUrl);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("Password Reset Request - File Management System");
            message.setText(buildResetEmailContent(resetLink));

            logger.debug("JavaMailSender instance: {}, Message: from={}, to={}, subject={}", 
                    javaMailSender != null ? "available" : "NULL", mailFrom, email, message.getSubject());
            
            javaMailSender.send(message);
            logger.info("Password reset email sent successfully to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}. Exception type: {}, Error: {}", 
                    email, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
        }
    }

private String buildResetEmailContent(String resetLink) {
    return "Dear User,\n\n" +
           "We have received a request to reset the password associated with your DocIT account. " +
           "If you did not initiate this request, please disregard this message and your account will remain secure.\n\n" +
           "To proceed with resetting your password, kindly click the secure link below:\n\n" +
           resetLink + "\n\n" +
           "For your security, this link will remain valid for " + TOKEN_EXPIRY_HOURS + " hours. " +
           "After it expires, you will need to submit a new password reset request.\n\n" +
           "If you did not request a password reset, no further action is required.\n\n" +
           "Thank you for trusting DocIT. We are committed to safeguarding your information and ensuring seamless access to your files.\n\n" +
           "Best regards,\n" +
           "DocIT Filemanagent Team"; // signature can be customized as needed
}

    @Transactional
    public void cleanupExpiredTokens() {
        passwordResetTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}








