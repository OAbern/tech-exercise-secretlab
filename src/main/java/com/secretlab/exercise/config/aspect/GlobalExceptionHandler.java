package com.secretlab.exercise.config.aspect;

import com.secretlab.exercise.model.vo.ErrorVO;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * Handles constraint violations from: - @Validated on controller path/query params → violations
   * set is populated - ValidationUtils.builder().validate() → violations set is empty; message is
   * pre-formatted
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorVO> handleConstraintViolation(ConstraintViolationException ex) {
    String message =
        ex.getConstraintViolations().isEmpty()
            ? ex.getMessage()
            : ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorVO(message));
  }

  /** Hand unexpected exception */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorVO> handleUnexpectedException(Exception ex) {
    log.error("unexpected exception found:", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorVO("Something is wrong!"));
  }
}
