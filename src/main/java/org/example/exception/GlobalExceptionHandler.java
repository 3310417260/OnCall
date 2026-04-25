package org.example.exception;

import org.example.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.UncheckedIOException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
        return buildErrorResponse(exception.getStatus(), exception.getMessage(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), null);
    }

    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<ApiResponse<Void>> handleUncheckedIOException(UncheckedIOException exception) {
        logger.error("处理请求时发生 IO 异常", exception);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception exception) {
        logger.error("处理请求时发生未捕获异常", exception);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(HttpStatus status, String message, Exception exception) {
        String responseMessage = (message == null || message.isBlank()) ? status.getReasonPhrase() : message;

        if (exception == null && status.is5xxServerError()) {
            logger.error("请求失败: status={}, message={}", status.value(), responseMessage);
        } else if (status.is4xxClientError()) {
            logger.warn("请求校验失败: status={}, message={}", status.value(), responseMessage);
        }

        return ResponseEntity.status(status).body(ApiResponse.error(status.value(), responseMessage));
    }
}
