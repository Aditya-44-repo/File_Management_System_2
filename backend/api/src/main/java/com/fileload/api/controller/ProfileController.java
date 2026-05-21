package com.fileload.api.controller;

import com.fileload.dao.repository.UserAccountRepository;
import com.fileload.model.dto.ProfileResponseDTO;
import com.fileload.model.dto.ProfileUpdateRequestDTO;
import com.fileload.model.dto.ProfileUserDTO;
import com.fileload.model.entity.UserAccount;
import com.fileload.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class ProfileController {

    private final ProfileService profileService;
    private final UserAccountRepository userAccountRepository;

    public ProfileController(ProfileService profileService,
                             UserAccountRepository userAccountRepository) {
        this.profileService = profileService;
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping
    @Operation(summary = "Get profile page information for current authenticated user")
    public ResponseEntity<ProfileResponseDTO> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

        ProfileResponseDTO profile = profileService.getProfile(user.getId());
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    @Operation(summary = "Update username/name of the current authenticated user")
    public ResponseEntity<ProfileUserDTO> updateProfile(@Valid @RequestBody ProfileUpdateRequestDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

        ProfileUserDTO updatedUser = profileService.updateProfile(user.getId(), request);
        return ResponseEntity.ok(updatedUser);
    }
}
