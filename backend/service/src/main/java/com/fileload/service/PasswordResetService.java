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
                                @Value("${mail.from:noreply@filemanagement.com}") String mailFrom) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userAccountRepository = userAccountRepository;
        this.javaMailSender = javaMailSender;
        this.passwordEncoder = passwordEncoder;
        this.frontendBaseUrl = frontendBaseUrl;
        this.mailFrom = mailFrom;
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

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("Password Reset Request - File Management System");
            message.setText(buildResetEmailContent(resetLink));

            logger.info("Sending password reset email to: {}", email);
            javaMailSender.send(message);
            logger.info("Password reset email sent successfully to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}. Error: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }

    private String buildResetEmailContent(String resetLink) {
        return "Hello,\n\n" +
                "We received a request to reset your password for your File Management System account. " +
                "If you did not make this request, you can safely ignore this email.\n\n" +
                "To reset your password, click the link below:\n" +
                resetLink + "\n\n" +
                "This link will expire in " + TOKEN_EXPIRY_HOURS + " hours.\n\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "File Management System Team";
    }

    @Transactional
    public void cleanupExpiredTokens() {
        passwordResetTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
