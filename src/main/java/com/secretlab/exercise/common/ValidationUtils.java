package com.secretlab.exercise.common;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;

/** Utility methods for building validation exceptions in service/controller layers. */
public final class ValidationUtils {

  private ValidationUtils() {}

  /** Builds a {@link ConstraintViolationException} for a single field violation. */
  public static ConstraintViolationException buildViolationEx(String field, String message) {
    return new ConstraintViolationException(field + ": " + message, Set.of());
  }
}
