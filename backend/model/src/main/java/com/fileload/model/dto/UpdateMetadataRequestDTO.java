package com.fileload.model.dto;

import java.util.List;

public record UpdateMetadataRequestDTO(String description, List<String> tags) {
}
