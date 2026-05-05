package com.fileload.api.controller;

import com.fileload.model.dto.DashboardOverviewDTO;
import com.fileload.model.dto.FileLoadResponseDTO;
import com.fileload.model.dto.SearchCriteriaDTO;
import com.fileload.model.dto.UpdateMetadataRequestDTO;
import com.fileload.model.dto.UpdateStatusRequestDTO;
import com.fileload.service.FileLoadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/file-loads")
@Tag(name = "File Loads")
public class FileLoadController {

    private static final Logger logger = LoggerFactory.getLogger(FileLoadController.class);

    private final FileLoadService fileLoadService;

    public FileLoadController(FileLoadService fileLoadService) {
        this.fileLoadService = fileLoadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Create new file load")
    public ResponseEntity<FileLoadResponseDTO> createFileLoad(@RequestParam("file") MultipartFile file,
                                                              @RequestParam(required = false) String description,
                                                              @RequestParam(required = false) java.util.List<String> tags) {
        FileLoadResponseDTO created = fileLoadService.createFileLoad(file);

        boolean hasDescription = description != null && !description.isBlank();
        boolean hasTags = tags != null && !tags.isEmpty();

        if (created.id() != null && (hasDescription || hasTags)) {
            UpdateMetadataRequestDTO metadata = new UpdateMetadataRequestDTO(hasDescription ? description.trim() : null, tags);
            created = fileLoadService.updateMetadata(created.id(), metadata);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Get file load by id")
    public ResponseEntity<FileLoadResponseDTO> getFileLoad(@PathVariable Long id) {
        return ResponseEntity.ok(fileLoadService.getFileLoadById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Search file loads")
    public ResponseEntity<Page<FileLoadResponseDTO>> searchFileLoads(
            @RequestParam(required = false) Long fileId,
            @RequestParam(required = false) String filename,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long recordCountMin,
            @RequestParam(required = false) Long recordCountMax,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "uploadDate,desc") String sort
    ) {
        SearchCriteriaDTO criteria = buildSearchCriteria(fileId, filename, status, startDate, endDate,
                recordCountMin, recordCountMax, page, size, sort);
        return ResponseEntity.ok(fileLoadService.searchFileLoads(criteria));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Search current user's file loads")
    public ResponseEntity<Page<FileLoadResponseDTO>> searchMyFileLoads(
            @RequestParam(required = false) Long fileId,
            @RequestParam(required = false) String filename,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long recordCountMin,
            @RequestParam(required = false) Long recordCountMax,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "uploadDate,desc") String sort
    ) {
        SearchCriteriaDTO criteria = buildSearchCriteria(fileId, filename, status, startDate, endDate,
                recordCountMin, recordCountMax, page, size, sort);
        return ResponseEntity.ok(fileLoadService.searchMyFileLoads(criteria));
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Get live dashboard overview metrics")
    public ResponseEntity<DashboardOverviewDTO> getDashboardOverview() {
        return ResponseEntity.ok(fileLoadService.getDashboardOverview());
    }

    private SearchCriteriaDTO buildSearchCriteria(Long fileId,
                                                  String filename,
                                                  String status,
                                                  String startDate,
                                                  String endDate,
                                                  Long recordCountMin,
                                                  Long recordCountMax,
                                                  Integer page,
                                                  Integer size,
                                                  String sort) {
        return new SearchCriteriaDTO(
                fileId,
                filename,
                null,
                status,
                parseDateTime(startDate),
                parseDateTime(endDate),
                recordCountMin,
                recordCountMax,
                page,
                size,
                sort
        );
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return java.time.LocalDate.parse(value).atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }

        throw new IllegalArgumentException("Invalid date format: " + value);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Update file status")
    public ResponseEntity<FileLoadResponseDTO> updateStatus(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateStatusRequestDTO request) {
        // Debug: log current user's authorities
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("[DEBUG] updateStatus called by user: {} with authorities: {}", auth.getName(), auth.getAuthorities());
        return ResponseEntity.ok(fileLoadService.updateFileLoadStatus(id, request.status(), request.comment()));
    }
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Update file metadata")
    public ResponseEntity<FileLoadResponseDTO> updateMetadata(@PathVariable Long id,
                                                              @RequestBody UpdateMetadataRequestDTO request) {
        return ResponseEntity.ok(fileLoadService.updateMetadata(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Delete file load")
    public ResponseEntity<Void> deleteFileLoad(@PathVariable Long id) {
        fileLoadService.deleteFileLoad(id);
        return ResponseEntity.noContent().build();
    }


//    @PostMapping("/{id}/retry")
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(summary = "Retry failed file load")
//    public ResponseEntity<FileLoadResponseDTO> retryFileLoad(@PathVariable Long id) {
//        return ResponseEntity.ok(fileLoadService.retryFileLoad(id));
//    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Download original file")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        FileLoadResponseDTO dto = fileLoadService.getFileLoadById(id);
        byte[] data = fileLoadService.downloadFile(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(dto.filename()).build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
