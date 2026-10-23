package com.designhub.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleOptimisticLockingFailureReturnsConflict409() {
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Row was updated or deleted by another transaction");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleOptimisticLockingFailure(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Conflict", response.getBody().get("error"));
        assertEquals("Dữ liệu đã bị thay đổi bởi thao tác khác. Vui lòng làm mới trang và thử lại.", response.getBody().get("message"));
    }

    @Test
    void testHandleResourceNotFoundExceptionReturns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: 99");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleResourceNotFoundException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Not Found", response.getBody().get("error"));
        assertEquals("Không tìm thấy đơn hàng với ID: 99", response.getBody().get("message"));
    }
}
