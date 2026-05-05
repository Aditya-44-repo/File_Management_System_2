package com.fileload.api.exception;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId,
        List<FieldViolation> errors
) {
    public record FieldViolation(String field, String message) {
    }
}

