package com.fileload.dao.repository;

import com.fileload.model.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUserIdAndIsUsedFalseAndExpiresAtAfter(Long userId, LocalDateTime now);

    void deleteByExpiresAtBefore(LocalDateTime now);

    void deleteByUserId(Long userId);
}
