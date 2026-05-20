package com.secretlab.exercise.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Static JSON utility.
 * ObjectMapper is thread-safe after construction and safe to share.
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    /**
     * Serialises a JsonNode to its JSON string representation.
     * e.g. TextNode("hi") → "\"hi\"", IntNode(42) → "42", ArrayNode → "[...]"
     *
     * @throws IllegalArgumentException if serialisation unexpectedly fails
     */
    public static String toJsonString(JsonNode node) {
        try {
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialise JSON value: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a JSON string back into a JsonNode.
     * Falls back to a plain TextNode if the string is not valid JSON.
     */
    public static JsonNode parseJson(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            return MAPPER.getNodeFactory().textNode(json);
        }
    }
}
