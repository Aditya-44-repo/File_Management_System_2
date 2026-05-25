package com.fileload.model.dto.admin;

import jakarta.validation.constraints.Pattern;

public record EmailChangeReviewRequestDTO(
        @Pattern(regexp = "approve|reject", message = "must be approve or reject")
        String action
) {
}
