package com.fileload.service.impl;

import com.fileload.dao.repository.EmailChangeRequestRepository;
import com.fileload.dao.repository.UserAccountRepository;
import com.fileload.model.dto.EmailChangeRequestDTO;
import com.fileload.model.entity.EmailChangeRequest;
import com.fileload.model.entity.EmailChangeRequestStatus;
import com.fileload.model.entity.UserAccount;
import com.fileload.service.EmailChangeRequestService;
import com.fileload.service.exception.ConflictException;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailChangeRequestServiceImpl implements EmailChangeRequestService {

    private final EmailChangeRequestRepository emailChangeRequestRepository;
    private final UserAccountRepository userAccountRepository;

    public EmailChangeRequestServiceImpl(EmailChangeRequestRepository emailChangeRequestRepository,
                                         UserAccountRepository userAccountRepository) {
        this.emailChangeRequestRepository = emailChangeRequestRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional
    public EmailChangeRequestDTO submit(Long userId, String newEmail) {
        UserAccount user = getUserOrThrow(userId);
        String normalizedEmail = normalizeEmail(newEmail);

        if (user.getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new ConflictException("New email must be different from current email");
        }
        if (emailChangeRequestRepository.existsByUserIdAndStatus(userId, EmailChangeRequestStatus.pending)) {
            throw new ConflictException("An email change request is already pending");
        }
        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email is already in use");
        }

        EmailChangeRequest request = new EmailChangeRequest();
        request.setUser(user);
        request.setOldEmail(user.getEmail());
        request.setNewEmail(normalizedEmail);
        request.setStatus(EmailChangeRequestStatus.pending);
        request.setRequestedAt(LocalDateTime.now());
        return toDto(emailChangeRequestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmailChangeRequestDTO> listPending() {
        return emailChangeRequestRepository.findByStatusOrderByRequestedAtAsc(EmailChangeRequestStatus.pending)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EmailChangeRequestDTO review(Long requestId, String action, Long reviewerId) {
        EmailChangeRequest request = emailChangeRequestRepository.findWithLockById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Email change request not found: " + requestId));

        if (request.getStatus() != EmailChangeRequestStatus.pending) {
            throw new ConflictException("Email change request has already been reviewed");
        }

        UserAccount reviewer = getUserOrThrow(reviewerId);
        String normalizedAction = action == null ? "" : action.trim().toLowerCase(Locale.ROOT);

        if ("approve".equals(normalizedAction)) {
            if (userAccountRepository.existsByEmail(request.getNewEmail())) {
                throw new ConflictException("Requested email is already in use");
            }
            UserAccount user = request.getUser();
            user.setEmail(request.getNewEmail());
            userAccountRepository.save(user);
            request.setStatus(EmailChangeRequestStatus.approved);
        } else if ("reject".equals(normalizedAction)) {
            request.setStatus(EmailChangeRequestStatus.rejected);
        } else {
            throw new IllegalArgumentException("Action must be approve or reject");
        }

        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(reviewer);
        return toDto(emailChangeRequestRepository.save(request));
    }

    @Override
    @Transactional
    public void cancel(Long userId, Long requestId) {
        EmailChangeRequest request = emailChangeRequestRepository.findWithLockById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Email change request not found: " + requestId));

        if (!request.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("Email change request not found: " + requestId);
        }
        if (request.getStatus() != EmailChangeRequestStatus.pending) {
            throw new ConflictException("Only pending email change requests can be cancelled");
        }

        emailChangeRequestRepository.delete(request);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmailChangeRequestDTO> latestForUser(Long userId) {
        Optional<EmailChangeRequest> pending = emailChangeRequestRepository
                .findFirstByUserIdAndStatusOrderByRequestedAtDesc(userId, EmailChangeRequestStatus.pending);
        if (pending.isPresent()) {
            return pending.map(this::toDto);
        }
        return emailChangeRequestRepository.findFirstByUserIdOrderByRequestedAtDesc(userId).map(this::toDto);
    }

    private UserAccount getUserOrThrow(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("New email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private EmailChangeRequestDTO toDto(EmailChangeRequest request) {
        UserAccount user = request.getUser();
        UserAccount reviewer = request.getReviewedBy();
        return new EmailChangeRequestDTO(
                request.getId(),
                user.getId(),
                user.getUsername(),
                request.getOldEmail(),
                request.getNewEmail(),
                request.getStatus().name(),
                request.getRequestedAt(),
                request.getReviewedAt(),
                reviewer == null ? null : reviewer.getId()
        );
    }
}
