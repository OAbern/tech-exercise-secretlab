package com.secretlab.exercise.config.aspect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerUnitTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("handleUnexpectedException returns generic 500 error")
  void handleUnexpectedException_returnsGenericInternalServerError() {
    var response = handler.handleUnexpectedException(new Exception("database password leaked"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().isSuccess()).isFalse();
    assertThat(response.getBody().getError()).isEqualTo("Something is wrong!");
  }
}
