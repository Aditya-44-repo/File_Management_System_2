package com.fileload.service.impl;

import com.fileload.dao.repository.FileLoadRepository;
import com.fileload.dao.repository.UserAccountRepository;
import com.fileload.dao.specification.FileLoadSpecifications;
import com.fileload.model.dto.DashboardOverviewDTO;
import com.fileload.model.dto.FileLoadResponseDTO;
import com.fileload.model.dto.SearchCriteriaDTO;
import com.fileload.model.dto.UpdateMetadataRequestDTO;
import com.fileload.model.entity.FileLoad;
import com.fileload.model.entity.FileStatus;
import com.fileload.model.entity.UserAccount;
import com.fileload.model.entity.UserRole;
import com.fileload.service.batch.BatchJobLauncherService;
import com.fileload.service.FileLoadService;
import com.fileload.service.mapper.FileLoadMapper;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileLoadServiceImpl implements FileLoadService {

    private final FileLoadRepository fileLoadRepository;
    private final UserAccountRepository userAccountRepository;
    private final FileLoadMapper fileLoadMapper;
    private final BatchJobLauncherService batchJobLauncherService;

    public FileLoadServiceImpl(FileLoadRepository fileLoadRepository,
                               UserAccountRepository userAccountRepository,
                               FileLoadMapper fileLoadMapper,
                               BatchJobLauncherService batchJobLauncherService) {
        this.fileLoadRepository = fileLoadRepository;
        this.userAccountRepository = userAccountRepository;
        this.fileLoadMapper = fileLoadMapper;
        this.batchJobLauncherService = batchJobLauncherService;
    }

    @Override
    public FileLoadResponseDTO createFileLoad(MultipartFile file) {
        return createFileLoad(file, null, null);
    }

    @Override
    public FileLoadResponseDTO createFileLoad(MultipartFile file, String description, java.util.List<String> tags) {
        String originalFilename = normalizeOriginalFilename(file.getOriginalFilename());
        String normalizedDescription = normalizeDescription(description);
        String normalizedTags = toTagCsv(tags);

        if (!originalFilename.toLowerCase().endsWith(".csv")) {
            return createFailedUploadResponse(file, originalFilename,
                    "Invalid file type. Only CSV files are accepted.",
                    normalizedDescription, normalizedTags);
        }

        if (file.isEmpty()) {
            return createFailedUploadResponse(file, originalFilename,
                    "File is empty.",
                    normalizedDescription, normalizedTags);
        }

        long maxSizeBytes = 20L * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            return createFailedUploadResponse(file, originalFilename,
                    "File size exceeded. Maximum allowed is 20MB.",
                    normalizedDescription, normalizedTags);
        }

        try {
            Path uploadsDir = Path.of("uploads");
            Files.createDirectories(uploadsDir);
            Path savedFile = resolveUniquePath(uploadsDir, originalFilename);
            Files.copy(file.getInputStream(), savedFile, StandardCopyOption.REPLACE_EXISTING);

            FileLoad entity = new FileLoad();
            entity.setFilename(originalFilename);
            entity.setFileType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            entity.setFileSize(file.getSize());
            entity.setLoadDate(LocalDateTime.now());
            entity.setStatus(FileStatus.PENDING);
            entity.setRecordCount(0L);
//            entity.setArchived(false);
            entity.setStoragePath(savedFile.toAbsolutePath().toString());
            entity.setDescription(normalizedDescription);
            entity.setTags(normalizedTags);
            applyCurrentUploader(entity);

            // Persist immediately so async batch thread can reliably load this record by id.
            FileLoad saved = fileLoadRepository.saveAndFlush(entity);
            launchBatch(saved.getId());
            return fileLoadMapper.toDto(saved);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store uploaded file", ex);
        }
    }

    private FileLoadResponseDTO createFailedUploadResponse(MultipartFile file, String filename, String errorMessage,
                                                           String description, String tagsCsv) {
        FileLoad failed = new FileLoad();
        failed.setFilename(filename);
        failed.setFileType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        failed.setFileSize(file.getSize());
        failed.setLoadDate(LocalDateTime.now());
        failed.setStatus(FileStatus.FAILED);
        failed.setRecordCount(0L);
        failed.setErrors(errorMessage);
//        failed.setArchived(false);
        failed.setStoragePath("");
        failed.setDescription(description);
        failed.setTags(tagsCsv);
        applyCurrentUploader(failed);

        return fileLoadMapper.toDto(fileLoadRepository.saveAndFlush(failed));
    }

    private String normalizeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "uploaded-file";
        }

        String cleaned = Path.of(originalFilename).getFileName().toString().trim();
        if (cleaned.isBlank()) {
            return "uploaded-file";
        }
        return cleaned;
    }

    private Path resolveUniquePath(Path uploadsDir, String originalFilename) {
        Path candidate = uploadsDir.resolve(originalFilename);
        if (!Files.exists(candidate)) {
            return candidate;
        }

        int dotIndex = originalFilename.lastIndexOf('.');
        String base = dotIndex > 0 ? originalFilename.substring(0, dotIndex) : originalFilename;
        String ext = dotIndex > 0 ? originalFilename.substring(dotIndex) : "";

        int counter = 1;
        while (true) {
            Path next = uploadsDir.resolve(base + "-" + counter + ext);
            if (!Files.exists(next)) {
                return next;
            }
            counter++;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FileLoadResponseDTO getFileLoadById(Long id) {
        FileLoad entity = fetchById(id);
        if (!canCurrentUserAccess(entity)) {
            throw new AccessDeniedException("You do not have permission to access this file.");
        }
        return fileLoadMapper.toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileLoadResponseDTO> searchFileLoads(SearchCriteriaDTO criteria) {
        return searchInternal(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileLoadResponseDTO> searchMyFileLoads(SearchCriteriaDTO criteria) {
        Long currentUserId = resolveCurrentUserId();
        if (currentUserId == null) {
            Pageable pageable = PageRequest.of(criteria.page(), criteria.size());
            return new PageImpl<>(java.util.List.of(), pageable, 0);
        }

        return searchInternal(criteria.withUploadedById(currentUserId));
    }

    @Override
    @Transactional
    public FileLoadResponseDTO updateFileLoadStatus(Long id, String status, String comment) {
        FileLoad entity = fetchById(id);
        entity.setStatus(FileStatus.valueOf(status.toUpperCase()));
        if (comment != null && !comment.isBlank()) {
            entity.setErrors(comment);
        }
        return fileLoadMapper.toDto(fileLoadRepository.save(entity));
    }

    @Override
    @Transactional
    public FileLoadResponseDTO updateMetadata(Long id, UpdateMetadataRequestDTO request) {
        FileLoad entity = fetchById(id);
        if (!canCurrentUserAccess(entity)) {
            throw new AccessDeniedException("You do not have permission to update this file.");
        }
        entity.setDescription(request.description());
        if (request.tags() != null) {
            String tagCsv = request.tags().stream()
                    .filter(tag -> tag != null && !tag.isBlank())
                    .map(String::trim)
                    .collect(Collectors.joining(","));
            entity.setTags(tagCsv);
        }
        return fileLoadMapper.toDto(fileLoadRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteFileLoad(Long id) {
        FileLoad entity = fetchById(id);
        if (!canCurrentUserAccess(entity)) {
            throw new AccessDeniedException("You do not have permission to delete this file.");
        }
        if (isOwnedBySuperAdmin(entity) && !isCurrentUserSuperAdmin()) {
            throw new AccessDeniedException("Files owned by SUPER_ADMIN can only be deleted by SUPER_ADMIN.");
        }
        if (entity.getStoragePath() != null) {
            try {
                Files.deleteIfExists(Path.of(entity.getStoragePath()));
            } catch (IOException ignored) {
            }
        }
        fileLoadRepository.delete(entity);
    }

//    @Override
//    @Transactional
//    public FileLoadResponseDTO archiveFileLoad(Long id) {
//        FileLoad entity = fetchById(id);
//        entity.setArchived(true);
//        entity.setStatus(FileStatus.ARCHIVED);
//        return fileLoadMapper.toDto(fileLoadRepository.save(entity));
//    }

    @Override
    @Transactional
    public FileLoadResponseDTO retryFileLoad(Long id) {
        FileLoad entity = fetchById(id);
        entity.setStatus(FileStatus.PENDING);
        entity.setErrors(null);
        entity = fileLoadRepository.save(entity);
        launchBatch(entity.getId());
        return fileLoadMapper.toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public long countFilesByUserId(Long userId) {
        return fileLoadRepository.countByUploadedById(userId);
    }

    @Override
    @Transactional
    public long deleteAllFilesByUserId(Long userId) {
        if (isTargetUserSuperAdmin(userId) && !isCurrentUserSuperAdmin()) {
            throw new AccessDeniedException("Files owned by SUPER_ADMIN can only be deleted by SUPER_ADMIN.");
        }
        List<FileLoad> files = fileLoadRepository.findByUploadedById(userId);
        for (FileLoad file : files) {
            if (file.getStoragePath() == null || file.getStoragePath().isBlank()) {
                continue;
            }
            try {
                Files.deleteIfExists(Path.of(file.getStoragePath()));
            } catch (IOException ignored) {
            }
        }
        fileLoadRepository.deleteAll(files);
        return files.size();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadFile(Long id) {
        FileLoad entity = fetchById(id);
        if (!canCurrentUserAccess(entity)) {
            throw new AccessDeniedException("You do not have permission to download this file.");
        }
        try {
            return Files.readAllBytes(Path.of(entity.getStoragePath()));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read file", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewDTO getDashboardOverview() {
        long totalUploads = fileLoadRepository.count();
        long pendingCount = fileLoadRepository.countByStatus(FileStatus.PENDING);
        long processingCount = fileLoadRepository.countByStatus(FileStatus.PROCESSING);
        long successCount = fileLoadRepository.countByStatus(FileStatus.SUCCESS);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();

        return new DashboardOverviewDTO(
                totalUploads,
                processingCount,
                totalUploads == 0 ? 0.0 : (successCount * 100.0) / totalUploads,
                fileLoadRepository.countByStatusAndLoadDateBetween(FileStatus.FAILED, startOfDay, now),
                pendingCount,
                processingCount,
                successCount,
                now
        );
    }

    private FileLoad fetchById(Long id) {
        return fileLoadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File load not found: " + id));
    }

    private String mapSort(String sortField) {
        return switch (sortField) {
            case "uploadDate" -> "loadDate";
            case "filename", "id", "status", "recordCount" -> sortField;
            default -> "loadDate";
        };
    }

    private void launchBatch(Long fileLoadId) {
        batchJobLauncherService.launch(fileLoadId);
    }

    private Page<FileLoadResponseDTO> searchInternal(SearchCriteriaDTO criteria) {
        String[] sortTokens = criteria.sort().split(",");
        String sortField = sortTokens[0];
        Sort.Direction direction = sortTokens.length > 1 && "asc".equalsIgnoreCase(sortTokens[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), Sort.by(direction, mapSort(sortField)));
        return fileLoadRepository.findAll(FileLoadSpecifications.withCriteria(criteria), pageable)
                .map(fileLoadMapper::toDto);
    }

    private void applyCurrentUploader(FileLoad fileLoad) {
        UserAccount currentUser = resolveCurrentUser();
        if (currentUser == null) {
            fileLoad.setUploadedById(null);
            fileLoad.setUploadedBy("SYSTEM");
            return;
        }

        fileLoad.setUploadedById(currentUser.getId());
        fileLoad.setUploadedBy(currentUser.getEmail());
    }

    private Long resolveCurrentUserId() {
        UserAccount currentUser = resolveCurrentUser();
        return currentUser == null ? null : currentUser.getId();
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String toTagCsv(java.util.List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }

        String csv = tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(","));

        return csv.isBlank() ? null : csv;
    }

    private UserAccount resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = authentication.getName();
        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            return null;
        }

        return userAccountRepository.findByEmail(email).orElse(null);
    }

    private boolean currentUserIdEquals(Long ownerId, Long currentUserId) {
        return ownerId != null && currentUserId != null && ownerId.equals(currentUserId);
    }

    private boolean canCurrentUserAccess(FileLoad fileLoad) {
        UserAccount currentUser = resolveCurrentUser();
        if (currentUser == null) {
            return false;
        }
        if (currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        return currentUserIdEquals(fileLoad.getUploadedById(), currentUser.getId());
    }

    private boolean isCurrentUserSuperAdmin() {
        UserAccount currentUser = resolveCurrentUser();
        return currentUser != null && currentUser.getRole() == UserRole.SUPER_ADMIN;
    }

    private boolean isOwnedBySuperAdmin(FileLoad fileLoad) {
        if (fileLoad.getUploadedById() == null) {
            return false;
        }
        return userAccountRepository.findById(fileLoad.getUploadedById())
                .map(user -> user.getRole() == UserRole.SUPER_ADMIN)
                .orElse(false);
    }

    private boolean isTargetUserSuperAdmin(Long userId) {
        return userAccountRepository.findById(userId)
                .map(user -> user.getRole() == UserRole.SUPER_ADMIN)
                .orElse(false);
    }
}

