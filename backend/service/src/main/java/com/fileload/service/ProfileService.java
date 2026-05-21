package com.fileload.service;

import com.fileload.model.dto.ProfileResponseDTO;
import com.fileload.model.dto.ProfileUserDTO;
import com.fileload.model.dto.ProfileUpdateRequestDTO;

public interface ProfileService {
    ProfileResponseDTO getProfile(Long userId);
    ProfileUserDTO updateProfile(Long userId, ProfileUpdateRequestDTO request);
}
