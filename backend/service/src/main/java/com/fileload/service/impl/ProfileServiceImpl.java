package com.fileload.service.impl;

import com.fileload.dao.repository.FileLoadRepository;
import com.fileload.dao.repository.LoginHistoryRepository;
import com.fileload.dao.repository.UserAccountRepository;
import com.fileload.model.dto.*;
import com.fileload.model.entity.FileLoad;
import com.fileload.model.entity.FileStatus;
import com.fileload.model.entity.LoginHistory;
import com.fileload.model.entity.UserAccount;
import com.fileload.service.ProfileService;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserAccountRepository userAccountRepository;
    private final FileLoadRepository fileLoadRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileServiceImpl(UserAccountRepository userAccountRepository,
                              FileLoadRepository fileLoadRepository,
                              LoginHistoryRepository loginHistoryRepository,
                              PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.fileLoadRepository = fileLoadRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponseDTO getProfile(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // 1. ProfileUserDTO
        ProfileUserDTO userDto = new ProfileUserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getProfileImage()
        );

        // 2. ProfileStatsDTO
        long totalUploads = fileLoadRepository.countByUploadedById(userId);
        long successfulUploads = fileLoadRepository.countByUploadedByIdAndStatus(userId, FileStatus.SUCCESS);
        long failedUploads = fileLoadRepository.countByUploadedByIdAndStatus(userId, FileStatus.FAILED);
        long totalDownloads = 0; // Downloads tracking can be added if needed, default to 0
        ProfileStatsDTO statsDto = new ProfileStatsDTO(totalUploads, totalDownloads, successfulUploads, failedUploads);

        // 3. ProfileActivityDTO List (latest 10 uploads)
        List<FileLoad> latestFiles = fileLoadRepository.findTop10ByUploadedByIdOrderByLoadDateDesc(userId);
        List<ProfileActivityDTO> activities = latestFiles.stream()
                .map(file -> {
                    String text;
                    String icon = "cloud_upload";
                    if (file.getStatus() == FileStatus.SUCCESS) {
                        text = "Uploaded " + file.getFilename();
                        icon = "upload";
                    } else if (file.getStatus() == FileStatus.FAILED) {
                        text = "Status failed for " + file.getFilename();
                        icon = "cloud_off";
                    } else {
                        text = "Processing " + file.getFilename();
                        icon = "cloud_queue";
                    }
                    return new ProfileActivityDTO(icon, text, file.getLoadDate());
                })
                .collect(Collectors.toList());

        // 4. LoginHistoryDTO List (latest 10 login events)
        List<LoginHistory> logins = loginHistoryRepository.findByUserIdOrderByTimeDesc(userId, PageRequest.of(0, 10));
        List<LoginHistoryDTO> loginHistoryDtos = logins.stream()
                .map(login -> new LoginHistoryDTO(
                        login.getDevice(),
                        login.getBrowser(),
                        login.getIp(),
                        login.getTime(),
                        login.getStatus()
                ))
                .collect(Collectors.toList());

        return new ProfileResponseDTO(userDto, statsDto, activities, loginHistoryDtos);
    }

    @Override
    @Transactional
    public ProfileUserDTO updateProfile(Long userId, ProfileUpdateRequestDTO request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        String newUsername = request.username().trim();
        Optional<UserAccount> existing = userAccountRepository.findByUsername(newUsername);
        if (existing.isPresent() && !existing.get().getId().equals(userId)) {
            throw new IllegalArgumentException("Username is already taken");
        }

        user.setUsername(newUsername);

        if (request.password() != null && !request.password().trim().isEmpty()) {
            if (request.currentPassword() == null || request.currentPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("Current password is required to set a new password");
            }

            if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new IllegalArgumentException("Current password is incorrect");
            }

            String newPassword = request.password().trim();
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        UserAccount saved = userAccountRepository.save(user);

        return new ProfileUserDTO(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getRole().name(),
                saved.getProfileImage()
        );
    }
}
