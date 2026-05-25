package com.fileload.service;

import com.fileload.model.dto.EmailChangeRequestDTO;
import java.util.List;
import java.util.Optional;

public interface EmailChangeRequestService {
    EmailChangeRequestDTO submit(Long userId, String newEmail);
    List<EmailChangeRequestDTO> listPending();
    EmailChangeRequestDTO review(Long requestId, String action, Long reviewerId);
    void cancel(Long userId, Long requestId);
    Optional<EmailChangeRequestDTO> latestForUser(Long userId);
}
