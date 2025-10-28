package com.javaguy.nhxserver.exception.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import com.javaguy.nhxserver.exception.TokenRefreshException;
import com.javaguy.nhxserver.exception.TokenExpiredException;
import com.javaguy.nhxserver.exception.TransactionFailedException;
import com.javaguy.nhxserver.exception.UserAlreadyExistsException;
import com.javaguy.nhxserver.exception.EmailNotVerifiedException;
import com.javaguy.nhxserver.exception.AccountDisabledException;
import com.javaguy.nhxserver.exception.EmailSendingException;
import com.javaguy.nhxserver.exception.PasswordsMismatchException;
import com.javaguy.nhxserver.exception.ResourceNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
            Map<String, Object> details) {
        ErrorResponse body = new ErrorResponse(new ErrorResponse.ErrorDetail(code, message, details));
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex,
            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to access this resource.",
                Map.of("path", request.getRequestURI()));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerifiedException(EmailNotVerifiedException ex,
            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", ex.getMessage(),
                Map.of("path", request.getRequestURI()));
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabledException(AccountDisabledException ex,
            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", ex.getMessage(),
                Map.of("path", request.getRequestURI()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex,
            HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "RESOURCE_ALREADY_EXISTS", ex.getMessage(),
                Map.of("path", request.getRequestURI()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(),
                Map.of("path", request.getRequestURI()));
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpiredException(TokenExpiredException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "TOKEN_EXPIRED", ex.getMessage(), Map.of("path", request.getRequestURI()));
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ErrorResponse> handleTokenRefreshException(TokenRefreshException ex,
            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "TOKEN_REFRESH_FAILED", ex.getMessage(),
                Map.of("path", request.getRequestURI()));
    }

    @ExceptionHandler(EmailSendingException.class)
    public ResponseEntity<ErrorResponse> handleEmailSendingException(EmailSendingException ex,
            HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_SENDING_FAILED", ex.getMessage(),
                Map.of("path", request.getRequestURI()));
    }

    @ExceptionHandler(PasswordsMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordsMismatchException(PasswordsMismatchException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "PASSWORDS_MISMATCH", ex.getMessage(),
                Map.of("path", request.getRequestURI()));
    }

    @ExceptionHandler(TransactionFailedException.class)
    public ResponseEntity<ErrorResponse> handleTransactionFailed(TransactionFailedException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "TRANSACTION_FAILED", ex.getMessage(),
                Map.of("transactionId", ex.getTransactionId(), "path", request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        for (var error : ex.getBindingResult().getAllErrors()) {
            if (error instanceof FieldError fe) {
                fieldErrors.put(fe.getField(), fe.getDefaultMessage());
            }
        }
        details.put("fields", fieldErrors);
        details.put("path", request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "One or more fields are invalid.", details);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", ex.getMessage(),
                Map.of("path", request.getRequestURI()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_ERROR",
                ex.getMessage() != null ? ex.getMessage() : "Authentication failed. Invalid credentials.",
                Map.of("path", request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred.",
                Map.of("path", request.getRequestURI()));
    }
}
