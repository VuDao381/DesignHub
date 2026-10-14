package com.designhub.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidationException(
                        MethodArgumentNotValidException ex) {

                Map<String, String> errors = new HashMap<>();

                ex.getBindingResult()
                                .getFieldErrors()
                                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

                Map<String, Object> response = new HashMap<>();

                response.put("timestamp", LocalDateTime.now());
                response.put("status", 400);
                response.put("error", "Validation Error");
                response.put("messages", errors);

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(response);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<Map<String, Object>> handleConstraintViolationException(
                        ConstraintViolationException ex) {

                Map<String, String> errors = new HashMap<>();

                ex.getConstraintViolations()
                                .forEach(violation -> errors.put(
                                                violation.getPropertyPath().toString(),
                                                violation.getMessage()));

                Map<String, Object> response = new HashMap<>();

                response.put("timestamp", LocalDateTime.now());
                response.put("status", 400);
                response.put("error", "Validation Error");
                response.put("messages", errors);

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(response);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
                        IllegalArgumentException ex) {

                Map<String, Object> response = new HashMap<>();

                response.put("timestamp", LocalDateTime.now());
                response.put("status", 400);
                response.put("error", "Bad Request");
                response.put("message", ex.getMessage());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(response);
        }

        @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
        public ResponseEntity<Map<String, Object>> handleAccessDenied(
                        org.springframework.security.access.AccessDeniedException ex) {

                Map<String, Object> response = new HashMap<>();
                response.put("timestamp", LocalDateTime.now());
                response.put("status", 403);
                response.put("error", "Forbidden");
                response.put("message", ex.getMessage());

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<Map<String, Object>> handleIllegalState(
                        IllegalStateException ex) {

                Map<String, Object> response = new HashMap<>();
                response.put("timestamp", LocalDateTime.now());
                response.put("status", 409);
                response.put("error", "Conflict");
                response.put("message", ex.getMessage());

                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
        public ResponseEntity<Map<String, Object>> handleAuthenticationException(
                        org.springframework.security.core.AuthenticationException ex) {

                Map<String, Object> response = new HashMap<>();
                response.put("timestamp", LocalDateTime.now());
                response.put("status", 401);
                response.put("error", "Unauthorized");
                response.put("message", ex.getMessage());

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
        public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
                        org.springframework.dao.DataIntegrityViolationException ex) {

                Map<String, Object> response = new HashMap<>();
                response.put("timestamp", LocalDateTime.now());
                response.put("status", 409);
                response.put("error", "Conflict");
                response.put("message", "Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc dữ liệu");

                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
        public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(
                        org.springframework.http.converter.HttpMessageNotReadableException ex) {

                Map<String, Object> response = new HashMap<>();
                response.put("timestamp", LocalDateTime.now());
                response.put("status", 400);
                response.put("error", "Bad Request");
                response.put("message", "Dữ liệu yêu cầu không hợp lệ hoặc sai định dạng JSON");

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleGeneralException(
                        Exception ex) {

                Map<String, Object> response = new HashMap<>();
                response.put("timestamp", LocalDateTime.now());
                response.put("status", 500);
                response.put("error", "Internal Server Error");
                response.put("message", "Đã có lỗi hệ thống xảy ra, vui lòng thử lại sau");

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
}
