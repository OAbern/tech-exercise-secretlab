package com.secretlab.exercise.model.vo;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response VO for GET /object (list all).
 *
 * <p>Serialises as { "<storeKey>": <parsedValue> } — a single-entry JSON object. The list produces:
 * [ {"k1": v1}, {"k2": v2}, ... ]
 */
@Setter
@NoArgsConstructor
public class KeyValuePairVO {

  private String key;
  private JsonNode value;

  @JsonAnyGetter
  public Map<String, JsonNode> serialize() {
    return Map.of(key, value != null ? value : NullNode.getInstance());
  }
}
