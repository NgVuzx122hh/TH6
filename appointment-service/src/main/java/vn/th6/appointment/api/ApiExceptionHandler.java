package vn.th6.appointment.api;

import vn.th6.appointment.integration.IntegrationConsistencyException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatusException(
            ResponseStatusException exception, HttpServletRequest request) {
        int status = exception.getStatusCode().value();
        ApiError error = new ApiError(Instant.now(), status, errorCode(status),
                exception.getReason() == null ? "Request could not be processed" : exception.getReason(),
                request.getRequestURI());
        return ResponseEntity.status(exception.getStatusCode()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Request validation failed");
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(IntegrationConsistencyException.class)
    public ResponseEntity<ApiError> handleConsistencyException(
            IntegrationConsistencyException exception, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "RECONCILIATION_REQUIRED",
                exception.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(
            AuthenticationException exception, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "Username or password is incorrect", request);
    }

    private ResponseEntity<ApiError> error(
            HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(),
                code, message, request.getRequestURI()));
    }

    private String errorCode(int status) {
        return switch (status) {
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 401 -> "UNAUTHORIZED";
            case 422 -> "INVALID_REQUEST";
            case 502 -> "PROVIDER_ERROR";
            case 503 -> "PROVIDER_UNAVAILABLE";
            default -> "REQUEST_ERROR";
        };
    }
}
