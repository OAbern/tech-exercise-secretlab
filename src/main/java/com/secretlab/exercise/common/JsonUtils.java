package com.secretlab.exercise.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/** Static JSON utility. ObjectMapper is thread-safe after construction and safe to share. */
@Slf4j
public final class JsonUtils {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private JsonUtils() {}

  /**
   * Serialises a JsonNode to its JSON string representation. e.g. TextNode("hi") → "\"hi\"",
   * IntNode(42) → "42", ArrayNode → "[...]"
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
   * Serialises a Object to its JSON string representation
   * if any exception, return Object.toString
   */
  public static String toJsonStringWithoutEx(Object obj) {
    if (obj == null) {
      return "null";
    }
    try {
      return MAPPER.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      log.warn("parse json string fail, obj: {}", obj, e);
      return String.valueOf(obj);
    }
  }

  public static String toJsonStringWithoutEx(Object obj, int maxStringLen) {
    String jsonString = toJsonStringWithoutEx(obj);

    if (jsonString.length() <= maxStringLen) {
      return jsonString;
    }

    return jsonString.substring(0, maxStringLen) + "...";
  }

  /**
   * Parses a JSON string back into a JsonNode. Falls back to a plain TextNode if the string is not
   * valid JSON.
   */
  public static JsonNode parseJson(String json) {
    try {
      return MAPPER.readTree(json);
    } catch (JsonProcessingException e) {
      log.error("parse json fail, json str: {}", json, e);
      return MAPPER.getNodeFactory().textNode(json);
    }
  }
}
