package com.secretlab.exercise.repository;

import com.secretlab.exercise.model.KeyValueHistory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KeyValueHistoryRepository extends JpaRepository<KeyValueHistory, Long> {

  /**
   * Entry that was current at the given Unix timestamp: Used only for point-in-time queries;
   * latest-value queries go to key_value.
   */
  @Query("SELECT e FROM KeyValueHistory e WHERE e.storeKey = :key AND e.createdAt = :timestamp")
  Optional<KeyValueHistory> findByKeyAtTimestamp(
      @Param("key") String key, @Param("timestamp") long timestamp);
}
