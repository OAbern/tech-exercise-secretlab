package com.secretlab.exercise.service;

import com.secretlab.exercise.common.Constants;
import com.secretlab.exercise.common.DuplicatedKeyBusinessException;
import com.secretlab.exercise.common.SpringContextUtils;
import com.secretlab.exercise.model.KeyValue;
import com.secretlab.exercise.model.KeyValueHistory;
import com.secretlab.exercise.model.dto.StoreRequest;
import com.secretlab.exercise.repository.KeyValueHistoryRepository;
import com.secretlab.exercise.repository.KeyValueRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeyValueService {

  private static final int MAX_RETRY_TIME_PUT = 2;

  private final KeyValueRepository keyValueRepository;
  private final KeyValueHistoryRepository keyValueHistoryRepository;

  public KeyValueHistory put(StoreRequest request) {
    int retryTime = MAX_RETRY_TIME_PUT;
    while (retryTime-- > 0) {
      try {
        return SpringContextUtils.getBean(KeyValueService.class).doPut(request);
      } catch (DuplicatedKeyBusinessException e) {
        log.info("retry put request: {}", request);
      }
    }

    throw new RuntimeException(String.format("put fail, request: %s", request));
  }

  /**
   * Write a new version for the given key.
   *
   * <p>Concurrency guarantee: the PESSIMISTIC_WRITE lock on the key_value row serialises all
   * concurrent writes to the same key. After a competing transaction commits and releases the lock,
   * the next waiter re-reads the already-updated current_version, so version numbers are always
   * sequential.
   *
   * <p>key_value is updated with the latest value for fast reads; key_value_history always receives
   * an append for point-in-time queries.
   */
  @Transactional
  public KeyValueHistory doPut(StoreRequest request) {
    long now = Instant.now().getEpochSecond();

    // 1. Lock the stable anchor row (SELECT FOR UPDATE), increment version,
    //    and cache the new value directly on key_value.
    Optional<KeyValue> existKeyVersion = keyValueRepository.findByKeyWithLock(request.getKey());
    int newVersion;
    if (existKeyVersion.isPresent()) {
      // 1.2 key exist, update
      KeyValue kv = existKeyVersion.get();
      newVersion = kv.getCurrentVersion() + 1;
      kv.setCurrentVersion(newVersion);
      kv.setValue(request.getValueJson());
      kv.setUpdatedAt(now);
      keyValueRepository.save(kv);
    } else {
      // 1.3 key is not exist, insert first
      try {
        keyValueRepository.save(new KeyValue(request.getKey(), 1, request.getValueJson(), now));
      } catch (DataIntegrityViolationException e) {
        if (e.getMessage().contains(Constants.JPA_DUPLICATED_KEY_STATE)) {
          log.warn("concurrent operation ex, request: {}", request, e);
          throw new DuplicatedKeyBusinessException(e.getMessage());
        } else {
          log.error("save keyValue ex, request: {}", request, e);
          throw e;
        }
      }

      newVersion = 1;
    }

    // 2. Append the immutable history entry.
    return keyValueHistoryRepository.save(
        new KeyValueHistory(request.getKey(), newVersion, request.getValueJson(), now));
  }

  /**
   * Returns the latest value by reading directly from key_value — no join with key_value_history
   * needed.
   */
  public Optional<KeyValueHistory> getLatest(String key) {
    return keyValueRepository
        .findByStoreKey(key)
        .map(
            kv ->
                new KeyValueHistory(
                    kv.getStoreKey(), kv.getCurrentVersion(), kv.getValue(), kv.getUpdatedAt()));
  }

  public Optional<KeyValueHistory> getAtTimestamp(String key, long timestamp) {
    return keyValueHistoryRepository.findByKeyAtTimestamp(key, timestamp);
  }

  /** Returns the latest value for every known key by scanning key_value */
  public List<KeyValue> getAllLatest() {
    return keyValueRepository.findAll();
  }
}
