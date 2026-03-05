package com.trackspace.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler
 * 
 * Handles all exceptions thrown by the application
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("message", ex.getMessage());
        errorDetails.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBadRequestException(
            BadRequestException ex, WebRequest request) {

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("message", ex.getMessage());
        errorDetails.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("message", ex.getMessage());
        errorDetails.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleDisabledException(
            DisabledException ex, WebRequest request) {

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("message", "Tài khoản đã bị khóa, vui lòng liên hệ quản trị viên");
        errorDetails.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(
                ApiResponse.error("Tài khoản đã bị khóa, vui lòng liên hệ quản trị viên"),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {

        return new ResponseEntity<>(
                ApiResponse.error("Email hoặc mật khẩu không đúng"),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleLockedException(
            LockedException ex, WebRequest request) {

        return new ResponseEntity<>(
                ApiResponse.error("Tài khoản đã bị khóa, vui lòng liên hệ quản trị viên"),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {

        return new ResponseEntity<>(
                ApiResponse.error("Xác thực thất bại: " + ex.getMessage()),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex, WebRequest request) {
        return new ResponseEntity<>(
                ApiResponse.error("Phương thức HTTP không được hỗ trợ: " + ex.getMethod()),
                HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException ex, WebRequest request) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Dữ liệu không hợp lệ");
        return new ResponseEntity<>(ApiResponse.error(msg), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex, WebRequest request) {
        return new ResponseEntity<>(
                ApiResponse.error("Bạn không có quyền thực hiện thao tác này"),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGlobalException(
            Exception ex, WebRequest request) {

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getDescription(false).replace("uri=", ""));

        // Determine user-friendly message based on exception type
        String userMessage;
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (ex instanceof org.springframework.transaction.CannotCreateTransactionException) {
            userMessage = "Không thể kết nối đến cơ sở dữ liệu. Vui lòng thử lại sau.";
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else if (ex.getCause() instanceof java.net.ConnectException
                || ex.getCause() instanceof java.sql.SQLException) {
            userMessage = "Lỗi kết nối cơ sở dữ liệu. Vui lòng thử lại sau.";
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else if (ex instanceof org.springframework.dao.DataIntegrityViolationException) {
            userMessage = "Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc.";
            status = HttpStatus.CONFLICT;
        } else if (ex instanceof IllegalArgumentException) {
            userMessage = "Dữ liệu không hợp lệ: " + ex.getMessage();
            status = HttpStatus.BAD_REQUEST;
        } else {
            userMessage = "Lỗi hệ thống: " + ex.getClass().getSimpleName() + " — " + ex.getMessage();
        }

        errorDetails.put("message", userMessage);

        // Log full error for debugging
        ex.printStackTrace();

        return new ResponseEntity<>(ApiResponse.error(userMessage), status);
    }
}
