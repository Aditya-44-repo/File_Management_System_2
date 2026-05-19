package com.fileload.api.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;


//Catches many common exceptions thrown by controllers/services and turns them into consistent HTTP responses with appropriate status codes and a JSON error body built by the private build(...) method.
//Logs the exception (at different levels depending on exception type).
//Preserves or generates a request trace id (X-Request-Id header or new UUID) and includes it in the response to help correlate logs and requests
//@RestControllerAdvice that centralizes exception-to-HTTP-response mapping for the API. It logs exceptions and returns a structured
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    //EntityNotFoundException -> 404 NOT FOUND
    //Logs at ERROR level and returns the exception message as the response message.
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        logger.error("Entity not found: {}", ex.getMessage(), ex);
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    //BadCredentialsException and AuthenticationException -> 401 UNAUTHORIZED
    //Logs at WARN and returns message "Invalid credentials" (hides exception message from client).
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(Exception ex, HttpServletRequest request) {
        logger.warn("Unauthorized access: {}", ex.getMessage(), ex);
        return build(HttpStatus.UNAUTHORIZED, "Invalid credentials", request, null);
    }

    //DisabledException -> 403 FORBIDDEN
    //Logs at WARN and returns the exception message (useful to indicate account disabled).
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabled(DisabledException ex, HttpServletRequest request) {
        logger.warn("Blocked account login attempt: {}", ex.getMessage(), ex);
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    //ccessDeniedException -> 403 FORBIDDEN
    //Logs at WARN and returns generic "You are not allowed to perform this action".
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(AccessDeniedException ex, HttpServletRequest request) {
        logger.warn("Forbidden access: {}", ex.getMessage(), ex);
        return build(HttpStatus.FORBIDDEN, "You are not allowed to perform this action", request, null);
    }

    //llegalArgumentException / IllegalStateException -> 400 BAD REQUEST
    //Logs at WARN and returns the exception message.
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        logger.warn("Bad request: {}", ex.getMessage(), ex);
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    //MethodArgumentNotValidException -> 400 BAD REQUEST (validation of request body)
    //Logs at WARN.
    //Builds a list of ApiErrorResponse.FieldViolation from BindingResult.getFieldErrors().
    //Response message is either "Validation failed" or the first field violation in the form "field message" (e.g., "email must not be blank").
    //Response body contains the list of field errors.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        logger.warn("Validation error: {}", ex.getMessage(), ex);
        List<ApiErrorResponse.FieldViolation> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiErrorResponse.FieldViolation(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        String message = errors.isEmpty() ? "Validation failed" : errors.get(0).field() + " " + errors.get(0).message();
        return build(HttpStatus.BAD_REQUEST, message, request, errors);
    }

    //ConstraintViolationException -> 400 BAD REQUEST (validation of parameters / path variables)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        logger.warn("Constraint violation: {}", ex.getMessage(), ex);
        List<ApiErrorResponse.FieldViolation> errors = ex.getConstraintViolations().stream()
                .map(v -> new ApiErrorResponse.FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .collect(Collectors.toList());
        String message = errors.isEmpty() ? "Validation failed" : errors.get(0).field() + " " + errors.get(0).message();
        return build(HttpStatus.BAD_REQUEST, message, request, errors);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            BindException.class
    })
    public ResponseEntity<ApiErrorResponse> handleRequestParsing(Exception ex, HttpServletRequest request) {
        logger.warn("Request parsing error: {}", ex.getMessage(), ex);
        return build(HttpStatus.BAD_REQUEST, "Invalid request payload or parameters", request, null);
    }

    //MaxUploadSizeExceededException -> 413 PAYLOAD TOO LARG
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        logger.warn("Max upload size exceeded: {}", ex.getMessage(), ex);
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file is too large", request, null);
    }
    //MultipartException -> 400 BAD REQUEST
    //Logs WARN and returns "Invalid multipart upload request".
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiErrorResponse> handleMultipart(MultipartException ex, HttpServletRequest request) {
        logger.warn("Multipart error: {}", ex.getMessage(), ex);
        return build(HttpStatus.BAD_REQUEST, "Invalid multipart upload request", request, null);
    }

    //DataIntegrityViolationException -> 409 CONFLICT
    //Logs ERROR and returns "Data conflict: duplicate or invalid value"
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        logger.error("Data integrity violation: {}", ex.getMessage(), ex);
        return build(HttpStatus.CONFLICT, "Data conflict: duplicate or invalid value", request, null);
    }

    //DataAccessException -> 500 INTERNAL SERVER ERROR
    //Logs ERROR and returns "Database operation failed".
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        logger.error("Data access error: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Database operation failed", request, null);
    }

    //Catch-all Exception -> 500 INTERNAL SERVER ERROR
    //Logs ERROR and returns "Unexpected error".
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request, null);
    }

    //How the build(...) method constructs the response
    //Reads header X-Request-Id from the incoming request; if absent or blank, generates a new UUID.
    //Creates an ApiErrorResponse with:
    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            List<ApiErrorResponse.FieldViolation> errors
    ) {
        String traceId = request.getHeader("X-Request-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        //Reads header X-Request-Id from the incoming request; if absent or blank, generates a new UUID.
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                traceId,
                errors
                //Instant.now() (timestamp)
                //status.value() (numeric HTTP status)
                //status.getReasonPhrase() (HTTP reason text)
                //message (as passed from handler)
                //request.getRequestURI() (path)
                //traceId (from header or UUID)
                //optional list of FieldViolation (for validation errors)
        );
        return ResponseEntity.status(status).body(body);
    }
}
