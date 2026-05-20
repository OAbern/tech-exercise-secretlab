package com.secretlab.exercise.repository;

import com.secretlab.exercise.model.KeyValue;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface KeyValueRepository extends JpaRepository<KeyValue, Long> {

    /**
     * Acquires a PESSIMISTIC_WRITE (SELECT FOR UPDATE) lock on the key_value row
     * for the given key. Concurrent callers block here until the lock is released,
     * then read the committed current_version — guaranteeing sequential increments.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT kv FROM KeyValue kv WHERE kv.storeKey = :key")
    Optional<KeyValue> findByKeyWithLock(@Param("key") String key);

    /** Non-locking lookup by store_key — used for read-only queries. */
    Optional<KeyValue> findByStoreKey(String storeKey);
}
