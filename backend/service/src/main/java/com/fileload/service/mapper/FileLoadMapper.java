package com.fileload.service.mapper;

import com.fileload.model.dto.FileLoadResponseDTO;
import com.fileload.model.entity.FileLoad;
import java.util.Arrays;
import java.util.Collections;
import org.springframework.stereotype.Component;

@Component
public class FileLoadMapper {

    public FileLoadResponseDTO toDto(FileLoad entity) {
        return new FileLoadResponseDTO(
                entity.getId(),
                entity.getFilename(),
                entity.getFileType(),
                entity.getFileSize(),
                entity.getStatus().name(),
                entity.getRecordCount(),
                entity.getErrors(),
                entity.getLoadDate(),
                entity.getUploadedById(),
                entity.getUploadedBy(),
                entity.getDescription(),
                entity.getTags() == null || entity.getTags().isBlank()
                        ? Collections.emptyList()
                        : Arrays.stream(entity.getTags().split(","))
                        .map(String::trim)
                        .filter(tag -> !tag.isBlank())
                        .toList()
        );
    }
}


