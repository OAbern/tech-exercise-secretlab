package com.secretlab.exercise.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.secretlab.exercise.common.JsonUtils;
import com.secretlab.exercise.common.ValidationUtils;
import com.secretlab.exercise.convertor.KeyValueConvertor;
import com.secretlab.exercise.model.dto.StoreRequest;
import com.secretlab.exercise.model.vo.KeyValuePairVO;
import com.secretlab.exercise.model.vo.KeyValueVO;
import com.secretlab.exercise.service.KeyValueService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST API for the version-controlled key-value store.
 */
@RestController
@RequestMapping("/object")
@RequiredArgsConstructor
@Validated
public class ObjectController {

    private final KeyValueService keyValueService;

    /**
     * POST /object
     * Body: { "mykey": <any JSON value> }
     *
     * Validation and k:v extraction are delegated to {@link #checkAndBuildStoreRequest}.
     * JSON conversion is handled by {@link JsonUtils}.
     */
    @PostMapping
    public ResponseEntity<KeyValueVO> store(
            @RequestBody @NotNull(message = "body must not be null") @NotEmpty(message = "body must not be empty") Map<String, JsonNode> body) {

        StoreRequest request = checkAndBuildStoreRequest(body);
        KeyValueVO vo = KeyValueConvertor.INSTANCE.toVO(keyValueService.put(request));
        return ResponseEntity.ok().body(vo);
    }

    /**
     * Validates the raw POST body and builds a {@link StoreRequest}.
     * {@link jakarta.validation.ConstraintViolationException} handled by GlobalExceptionHandler.
     */
    private StoreRequest checkAndBuildStoreRequest(Map<String, JsonNode> body) {
        Map.Entry<String, JsonNode> first = body.entrySet().iterator().next();
        String key = first.getKey();
        JsonNode valueNode = first.getValue();

        if (body.size() > 1) {
            throw ValidationUtils.buildViolationEx("body", "body should only contains one kv");
        }
        if (key.isBlank()) {
            throw ValidationUtils.buildViolationEx("key", "must not be blank");
        }
        if (valueNode == null || valueNode.isNull()) {
            throw ValidationUtils.buildViolationEx("value", "must not be null");
        }

        return new StoreRequest(key, JsonUtils.toJsonString(valueNode));
    }

    /**
     * GET /object/get_all_records
     * Returns the latest version of every key as a compact list.
     * Response: [ {"k1": v1}, {"k2": v2}, ... ]
     */
    @GetMapping("get_all_records")
    public List<KeyValuePairVO> listAll() {
        return KeyValueConvertor.INSTANCE.toPairVOList(keyValueService.getAllLatest());
    }

    /**
     * GET /object/{key}              — latest value for the key.
     * GET /object/{key}?timestamp=N  — value the key held at Unix timestamp N (UTC).
     * Response: { key, value, version, timestamp }
     */
    @GetMapping("/{key}")
    public ResponseEntity<KeyValueVO> get(
            @PathVariable @NotBlank(message = "key must not be blank") String key,
            @RequestParam(required = false) @Positive(message = "timestamp must be a positive Unix epoch value") Long timestamp) {

        return (timestamp != null
                ? keyValueService.getAtTimestamp(key, timestamp)
                : keyValueService.getLatest(key))
                .map(entry -> ResponseEntity.ok(KeyValueConvertor.INSTANCE.toVO(entry)))
                .orElseGet(() -> ResponseEntity.<KeyValueVO>ok().build());
    }
}
