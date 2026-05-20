package com.secretlab.exercise.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response VO for error cases.
 */
@Getter
@AllArgsConstructor
public class ErrorVO {
    private final boolean success = false;
    private final String error;
}
