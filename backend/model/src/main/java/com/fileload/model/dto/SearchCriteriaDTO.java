package com.fileload.model.dto;

import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

public record SearchCriteriaDTO(
        Long fileId,
        String filename,
        Long uploadedById,
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long recordCountMin,
        Long recordCountMax,
        @Min(0) Integer page,
        @Min(1) Integer size,
        String sort
) {
    public SearchCriteriaDTO {
        page = page == null ? 0 : page;
        size = size == null ? 10 : size;
        sort = (sort == null || sort.isBlank()) ? "loadDate,desc" : sort;
    }

    public SearchCriteriaDTO withUploadedById(Long userId) {
        return new SearchCriteriaDTO(fileId, filename, userId, status, startDate, endDate, recordCountMin, recordCountMax,
                page, size, sort);
    }
}
