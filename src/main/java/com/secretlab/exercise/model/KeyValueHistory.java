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
 * Append-only history table. One row per (key, version) pair.
 * Rows are never updated or deleted.
 */
@Entity
@Table(name = "key_value_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KeyValueHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_key", nullable = false, length = 255)
    private String storeKey;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "stored_value", nullable = false, columnDefinition = "CLOB")
    private String value;

    /** Unix epoch seconds (UTC). */
    @Column(name = "created_at", nullable = false)
    private long createdAt;

    public KeyValueHistory(String storeKey, int version, String value, long createdAt) {
        this.storeKey = storeKey;
        this.version = version;
        this.value = value;
        this.createdAt = createdAt;
    }
}
