-- ─── Version-controlled key-value store ─────────────────────────────────────

-- Lock anchor table: one row per key.
-- Holds the current version counter and a cached copy of the latest value.
CREATE TABLE IF NOT EXISTS key_value (
    id              BIGINT AUTO_INCREMENT,
    store_key       VARCHAR(255) NOT NULL,
    current_version INT          NOT NULL,
    latest_value    CLOB         NOT NULL,
    updated_at      BIGINT       NOT NULL,
    CONSTRAINT pk_kv   PRIMARY KEY (id),
    CONSTRAINT uk_store_key UNIQUE (store_key)
);

-- Append-only history table: one row per (key, version) pair.
-- Rows are never updated or deleted.
CREATE TABLE IF NOT EXISTS key_value_history (
    id           BIGINT AUTO_INCREMENT,
    store_key    VARCHAR(255) NOT NULL,
    version      INT          NOT NULL,
    stored_value CLOB         NOT NULL,
    created_at   BIGINT       NOT NULL,
    CONSTRAINT pk_kvh PRIMARY KEY (id)
);
