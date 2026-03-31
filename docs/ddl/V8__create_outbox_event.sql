CREATE TABLE outbox_event (
    id             VARCHAR(36)  NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   VARCHAR(100) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSON         NOT NULL,
    partition_key  VARCHAR(100) NOT NULL,
    topic          VARCHAR(100) NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    published_at   DATETIME(6)  NULL,
    INDEX idx_outbox_unpublished (published_at, created_at)
);
