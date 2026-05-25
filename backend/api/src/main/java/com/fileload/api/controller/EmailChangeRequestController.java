package com.fileload.api.controller;

import com.fileload.dao.repository.UserAccountRepository;
import com.fileload.model.dto.EmailChangeRequestDTO;
import com.fileload.model.dto.EmailChangeRequestSubmitDTO;
import com.fileload.model.entity.UserAccount;
import com.fileload.service.EmailChangeRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email-change-requests")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
@Tag(name = "Email Change Requests")
public class EmailChangeRequestController {

    private final EmailChangeRequestService emailChangeRequestService;
    private final UserAccountRepository userAccountRepository;

    public EmailChangeRequestController(EmailChangeRequestService emailChangeRequestService,
                                        UserAccountRepository userAccountRepository) {
        this.emailChangeRequestService = emailChangeRequestService;
        this.userAccountRepository = userAccountRepository;
    }

    @PostMapping
    @Operation(summary = "Submit an email change request for the current user")
    public ResponseEntity<EmailChangeRequestDTO> submit(@Valid @RequestBody EmailChangeRequestSubmitDTO request) {
        UserAccount user = currentUser();
        EmailChangeRequestDTO created = emailChangeRequestService.submit(user.getId(), request.newEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel the current user's pending email change request")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        UserAccount user = currentUser();
        emailChangeRequestService.cancel(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    private UserAccount currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));
    }
}
