package com.secretlab.exercise.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per key. Acts as both the pessimistic-lock anchor for concurrent
 * version increments and a fast-path cache for the latest value.
 */
@Entity
@Table(name = "key_value")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KeyValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_key", nullable = false, unique = true, length = 255)
    private String storeKey;

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    /** Latest JSON value for this key (mirrors the most recent key_value_history row). */
    @Column(name = "latest_value", nullable = false, columnDefinition = "CLOB")
    private String value;

    /** Unix epoch seconds (UTC) of the latest write. */
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    public KeyValue(String storeKey, int currentVersion, String value, long updatedAt) {
        this.storeKey = storeKey;
        this.currentVersion = currentVersion;
        this.value = value;
        this.updatedAt = updatedAt;
    }
}
