package com.secretlab.exercise.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.secretlab.exercise.model.KeyValue;
import com.secretlab.exercise.model.KeyValueHistory;
import com.secretlab.exercise.repository.KeyValueHistoryRepository;
import com.secretlab.exercise.repository.KeyValueRepository;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ObjectControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private KeyValueRepository keyVersionRepository;

  @Autowired private KeyValueHistoryRepository keyValueRepository;

  @BeforeEach
  void setUp() {
    keyValueRepository.deleteAll();
    keyVersionRepository.deleteAll();
  }

  // ─── POST /object ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("POST /object — new key is created at version 1")
  void store_newKey_returnsVersion1() throws Exception {
    mockMvc
        .perform(
            post("/object")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mykey\": \"myvalue\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.key").value("mykey"))
        .andExpect(jsonPath("$.value").value("myvalue"))
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.timestamp").isNumber());
  }

  @Test
  @DisplayName("POST /object — existing key version is incremented by 1")
  void store_existingKey_incrementsVersion() throws Exception {
    mockMvc
        .perform(
            post("/object")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mykey\": \"value1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1));

    mockMvc
        .perform(
            post("/object")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mykey\": \"value2\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.value").value("value2"));
  }

  @Test
  @DisplayName("POST /object — empty body returns 400")
  void store_emptyBody_returns400() throws Exception {
    mockMvc
        .perform(post("/object").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").isString());
  }

  @Test
  @DisplayName("POST /object — blank key returns 400")
  void store_blankKey_returns400() throws Exception {
    mockMvc
        .perform(
            post("/object").contentType(MediaType.APPLICATION_JSON).content("{\"  \": \"value\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").isString());
  }

  @Test
  @DisplayName("POST /object — null value returns 400")
  void store_nullValue_returns400() throws Exception {
    mockMvc
        .perform(
            post("/object").contentType(MediaType.APPLICATION_JSON).content("{\"mykey\": null}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").isString());
  }

  @Test
  @DisplayName("POST /object — value can be a JSON array")
  void store_arrayValue_returnsVersion1() throws Exception {
    mockMvc
        .perform(
            post("/object")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mykey\": [1, 2, 3]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.key").value("mykey"))
        .andExpect(jsonPath("$.value[0]").value(1))
        .andExpect(jsonPath("$.version").value(1));
  }

  @Test
  @DisplayName("POST /object — value can be a JSON object")
  void store_objectValue_returnsVersion1() throws Exception {
    mockMvc
        .perform(
            post("/object")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mykey\": {\"a\": 1, \"b\": \"hello\"}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.key").value("mykey"))
        .andExpect(jsonPath("$.value.a").value(1))
        .andExpect(jsonPath("$.value.b").value("hello"))
        .andExpect(jsonPath("$.version").value(1));
  }

  @Test
  @DisplayName("POST /object — value can be a number")
  void store_numberValue_returnsVersion1() throws Exception {
    mockMvc
        .perform(post("/object").contentType(MediaType.APPLICATION_JSON).content("{\"mykey\": 42}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value(42))
        .andExpect(jsonPath("$.version").value(1));
  }

  // ─── GET /object/{key} ─────────────────────────────────────────────────────

  @Test
  @DisplayName("GET /object/{key} — returns latest value")
  void getLatest_existingKey_returnsLatestEntry() throws Exception {
    mockMvc
        .perform(
            post("/object").contentType(MediaType.APPLICATION_JSON).content("{\"hero\": \"v1\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/object").contentType(MediaType.APPLICATION_JSON).content("{\"hero\": \"v2\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/object/hero"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value("v2"));
  }

  @Test
  @DisplayName("GET /object/{key} — returns stored JSON object as response body")
  void getLatest_objectValue_returnsValueObject() throws Exception {
    mockMvc
        .perform(
            post("/object")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hero\": {\"name\": \"batman\", \"level\": 7}}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/object/hero"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("batman"))
        .andExpect(jsonPath("$.level").value(7))
        .andExpect(jsonPath("$.key").doesNotExist())
        .andExpect(jsonPath("$.value").doesNotExist());
  }

  @Test
  @DisplayName("GET /object/{key} — unknown key returns null")
  void getLatest_unknownKey_returnsNull() throws Exception {
    mockMvc
        .perform(get("/object/nonexistent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  @DisplayName("GET /object/{key}?timestamp — returns value active at that time")
  void getAtTimestamp_returnsCorrectVersion() throws Exception {
    long t1 = 1_000_000L;
    long t2 = 2_000_000L;

    // Insert history directly with controlled timestamps (avoids sleep in tests)
    keyVersionRepository.save(new KeyValue("histkey", 2, "value2", t2));
    keyValueRepository.save(new KeyValueHistory("histkey", 1, "value1", t1));
    keyValueRepository.save(new KeyValueHistory("histkey", 2, "value2", t2));

    mockMvc
        .perform(get("/object/histkey").param("timestamp", String.valueOf(t1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value("value1"));

    mockMvc
        .perform(get("/object/histkey").param("timestamp", String.valueOf(t2)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value("value2"));
  }

  @Test
  @DisplayName("GET /object/{key}?timestamp — key had no value at given time returns null")
  void getAtTimestamp_keyNotYetExisted_returnsNull() throws Exception {
    long t1 = 1_000_000L;
    long t2 = 2_000_000L;

    keyVersionRepository.save(new KeyValue("latekey", 1, "value1", t2));
    keyValueRepository.save(new KeyValueHistory("latekey", 1, "value1", t2));

    // Query before the key was written
    mockMvc
        .perform(get("/object/latekey").param("timestamp", String.valueOf(t1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  @DisplayName("GET /object/{key}?timestamp — negative timestamp returns 400")
  void getAtTimestamp_negativeTimestamp_returns400() throws Exception {
    mockMvc
        .perform(get("/object/mykey").param("timestamp", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").isString());
  }

  // ─── GET /object/get_all_records ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("GET /object/get_all_records — empty store returns empty list")
  void listAll_noData_returnsEmptyList() throws Exception {
    mockMvc
        .perform(get("/object/get_all_records"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName("GET /object — returns latest version per key")
  void listAll_withMultipleKeys_returnsLatestPerKey() throws Exception {
    mockMvc
        .perform(
            post("/object").contentType(MediaType.APPLICATION_JSON).content("{\"alpha\": \"a1\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/object").contentType(MediaType.APPLICATION_JSON).content("{\"alpha\": \"a2\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/object").contentType(MediaType.APPLICATION_JSON).content("{\"beta\": \"b1\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/object/get_all_records"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[?(@.alpha)].alpha").value("a2"))
        .andExpect(jsonPath("$[?(@.beta)].beta").value("b1"));
  }

  // ─── Concurrency ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("POST /object — 50 concurrent writes produce sequential versions 1-50")
  void store_50ConcurrentRequests_versionsAreSequential() throws Exception {
    int count = 50;
    ExecutorService executor = Executors.newFixedThreadPool(50);
    CountDownLatch startGate = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(count);

    for (int i = 0; i < count; i++) {
      String v = "value" + i;
      executor.submit(
          () -> {
            try {
              startGate.await();
              mockMvc
                  .perform(
                      post("/object")
                          .contentType(MediaType.APPLICATION_JSON)
                          .content("{\"concurrentKey\": \"" + v + "\"}"))
                  .andExpect(status().isOk());
            } catch (Exception e) {
              throw new RuntimeException(e);
            } finally {
              done.countDown();
            }
          });
    }

    startGate.countDown();
    boolean finished = done.await(30, TimeUnit.SECONDS);
    executor.shutdown();

    assertThat(finished).as("All 50 requests should complete within 30 seconds").isTrue();

    List<KeyValueHistory> entries =
        keyValueRepository.findAll().stream()
            .filter(e -> e.getStoreKey().equals("concurrentKey"))
            .sorted(Comparator.comparingInt(KeyValueHistory::getVersion))
            .toList();

    assertThat(entries).hasSize(count);
    for (int i = 0; i < count; i++) {
      assertThat(entries.get(i).getVersion())
          .as("Version at index %d should be %d", i, i + 1)
          .isEqualTo(i + 1);
    }
  }
}
