package com.fileload.service;

import com.fileload.model.dto.DashboardOverviewDTO;
import com.fileload.model.dto.FileLoadResponseDTO;
import com.fileload.model.dto.SearchCriteriaDTO;
import com.fileload.model.dto.UpdateMetadataRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface FileLoadService {

    FileLoadResponseDTO createFileLoad(MultipartFile file);

    default FileLoadResponseDTO createFileLoad(MultipartFile file, String description, java.util.List<String> tags) {
        return createFileLoad(file);
    }

    FileLoadResponseDTO getFileLoadById(Long id);

    Page<FileLoadResponseDTO> searchFileLoads(SearchCriteriaDTO criteria);

    Page<FileLoadResponseDTO> searchMyFileLoads(SearchCriteriaDTO criteria);

    FileLoadResponseDTO updateFileLoadStatus(Long id, String status, String comment);

    FileLoadResponseDTO updateMetadata(Long id, UpdateMetadataRequestDTO request);

    void deleteFileLoad(Long id);

//    FileLoadResponseDTO archiveFileLoad(Long id);

//    FileLoadResponseDTO retryFileLoad(Long id);

    FileLoadResponseDTO retryFileLoad(Long id);

    long countFilesByUserId(Long userId);

    long deleteAllFilesByUserId(Long userId);

    byte[] downloadFile(Long id);

    DashboardOverviewDTO getDashboardOverview();
}
