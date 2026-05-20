package com.secretlab.exercise.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Input DTO for POST /object.
 * The controller parses the raw JSON body (format: {"<key>": "<value>"})
 */
@Getter
@AllArgsConstructor
public class StoreRequest {

    @NotBlank(message = "key must not be blank")
    private final String key;

    @NotBlank(message = "value must not be blank")
    private final String valueJson;
}
