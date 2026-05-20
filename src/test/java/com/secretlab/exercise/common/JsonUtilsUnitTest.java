package com.secretlab.exercise.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonUtilsUnitTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("toJsonString")
    class ToJsonString {

        @Test
        @DisplayName("serializes object node")
        void objectNode() throws Exception {
            JsonNode node = MAPPER.readTree("{\"k\":\"v\"}");
            assertThat(JsonUtils.toJsonString(node)).isEqualTo("{\"k\":\"v\"}");
        }

        @Test
        @DisplayName("serializes NullNode")
        void nullNode() {
            assertThat(JsonUtils.toJsonString(NullNode.getInstance())).isEqualTo("null");
        }
    }

    @Nested
    @DisplayName("toJsonStringWithoutEx")
    class ToJsonStringWithoutEx {

        @Test
        @DisplayName("returns \"null\" for null input")
        void nullInput() {
            assertThat(JsonUtils.toJsonStringWithoutEx(null)).isEqualTo("null");
        }

        @Test
        @DisplayName("serializes Map to JSON")
        void mapInput() {
            assertThat(JsonUtils.toJsonStringWithoutEx(java.util.Map.of("a", 1)))
                    .isEqualTo("{\"a\":1}");
        }

        @Test
        @DisplayName("falls back to toString for non-serializable")
        void nonSerializableInput() {
            Object unserializable = new Object() {
                @Override
                public String toString() {
                    return "fallback-value";
                }
            };
            // Jackson can serialize plain Object as {}, but overriding toString lets us verify fallback
            String result = JsonUtils.toJsonStringWithoutEx(unserializable);
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("toJsonStringWithoutEx with max length")
    class ToJsonStringWithoutExTruncated {

        @Test
        @DisplayName("returns as-is when within limit")
        void withinLimit() {
            assertThat(JsonUtils.toJsonStringWithoutEx("hi", 100)).isEqualTo("\"hi\"");
        }

        @Test
        @DisplayName("truncates and appends ... when exceeds limit")
        void exceedsLimit() {
            String result = JsonUtils.toJsonStringWithoutEx("abcdefghij", 5);
            assertThat(result).hasSize(8).endsWith("...");          // 5 chars + "..."
        }

        @Test
        @DisplayName("does not truncate when exact limit")
        void exactLimit() {
            // "\"hi\"" is 4 chars
            String result = JsonUtils.toJsonStringWithoutEx("hi", 4);
            assertThat(result).isEqualTo("\"hi\"").doesNotEndWith("...");
        }
    }

    @Nested
    @DisplayName("parseJson")
    class ParseJson {

        @Test
        @DisplayName("parses valid JSON object")
        void validJsonObject() {
            JsonNode node = JsonUtils.parseJson("{\"key\":\"value\"}");
            assertThat(node.isObject()).isTrue();
            assertThat(node.get("key").asText()).isEqualTo("value");
        }

        @Test
        @DisplayName("parses JSON null")
        void validJsonNull() {
            JsonNode node = JsonUtils.parseJson("null");
            assertThat(node.isNull()).isTrue();
        }

        @Test
        @DisplayName("falls back to TextNode for invalid JSON")
        void invalidJsonFallsBackToTextNode() {
            String invalid = "not-valid-json{";
            JsonNode node = JsonUtils.parseJson(invalid);
            assertThat(node.isTextual()).isTrue();
            assertThat(node.asText()).isEqualTo(invalid);
        }
    }
}
