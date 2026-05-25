package com.fileload.dao.repository;

import com.fileload.model.entity.EmailChangeRequest;
import com.fileload.model.entity.EmailChangeRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface EmailChangeRequestRepository extends JpaRepository<EmailChangeRequest, Long> {

    boolean existsByUserIdAndStatus(Long userId, EmailChangeRequestStatus status);

    Optional<EmailChangeRequest> findFirstByUserIdAndStatusOrderByRequestedAtDesc(
            Long userId,
            EmailChangeRequestStatus status
    );

    Optional<EmailChangeRequest> findFirstByUserIdOrderByRequestedAtDesc(Long userId);

    Optional<EmailChangeRequest> findFirstByOldEmailAndStatusOrderByReviewedAtDesc(
            String oldEmail,
            EmailChangeRequestStatus status
    );

    List<EmailChangeRequest> findByStatusOrderByRequestedAtAsc(EmailChangeRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailChangeRequest> findWithLockById(Long id);
}
