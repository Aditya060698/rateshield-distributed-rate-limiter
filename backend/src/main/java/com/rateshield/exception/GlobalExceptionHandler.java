package com.rateshield.exception;

import com.rateshield.dto.response.ApiError;
import com.rateshield.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * A global exception handler keeps error responses consistent across the application.
 * Without it, every controller would need to duplicate error-mapping logic.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
        ApiError error = new ApiError(
                exception.getCode(),
                exception.getMessage(),
                List.of(),
                Instant.now()
        );

        return ResponseEntity
                .status(exception.getStatus())
                .body(ApiResponse.failure(exception.getMessage(), error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .toList();

        ApiError error = new ApiError(
                "VALIDATION_ERROR",
                "Request validation failed",
                details,
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("Validation failed", error));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException exception) {
        List<String> details = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        ApiError error = new ApiError(
                "CONSTRAINT_VIOLATION",
                "Constraint validation failed",
                details,
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("Constraint validation failed", error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandledException(Exception exception) {
        ApiError error = new ApiError(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                List.of(exception.getMessage()),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Internal server error", error));
    }
}
