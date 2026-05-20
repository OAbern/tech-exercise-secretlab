package com.secretlab.exercise.service;

import com.secretlab.exercise.model.KeyValue;
import com.secretlab.exercise.model.KeyValueHistory;
import com.secretlab.exercise.model.dto.StoreRequest;
import com.secretlab.exercise.repository.KeyValueHistoryRepository;
import com.secretlab.exercise.repository.KeyValueRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KeyValueService {

  private final KeyValueRepository keyVersionRepository;
  private final KeyValueHistoryRepository keyValueRepository;

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
  public KeyValueHistory put(StoreRequest request) {
    long now = Instant.now().getEpochSecond();

    // 1. Lock the stable anchor row (SELECT FOR UPDATE), increment version,
    //    and cache the new value directly on key_value.
    Optional<KeyValue> existKeyVersion = keyVersionRepository.findByKeyWithLock(request.getKey());
    int newVersion;
    if (existKeyVersion.isPresent()) {
      KeyValue kv = existKeyVersion.get();
      newVersion = kv.getCurrentVersion() + 1;
      kv.setCurrentVersion(newVersion);
      kv.setValue(request.getValueJson());
      kv.setUpdatedAt(now);
      keyVersionRepository.save(kv);
    } else {
      // TODO duplicate Ex
      keyVersionRepository.save(new KeyValue(request.getKey(), 1, request.getValueJson(), now));
      newVersion = 1;
    }

    // 2. Append the immutable history entry.
    return keyValueRepository.save(
        new KeyValueHistory(request.getKey(), newVersion, request.getValueJson(), now));
  }

  /**
   * Returns the latest value by reading directly from key_value — no join with key_value_history
   * needed.
   */
  public Optional<KeyValueHistory> getLatest(String key) {
    return keyVersionRepository
        .findByStoreKey(key)
        .map(
            kv ->
                new KeyValueHistory(
                    kv.getStoreKey(), kv.getCurrentVersion(), kv.getValue(), kv.getUpdatedAt()));
  }

  public Optional<KeyValueHistory> getAtTimestamp(String key, long timestamp) {
    return keyValueRepository.findByKeyAtTimestamp(key, timestamp);
  }

  /** Returns the latest value for every known key by scanning key_value */
  public List<KeyValue> getAllLatest() {
    return keyVersionRepository.findAll();
  }
}
