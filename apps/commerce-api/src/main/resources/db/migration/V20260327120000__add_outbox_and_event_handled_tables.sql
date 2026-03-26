-- Create outbox table for event sourcing
CREATE TABLE outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    topic VARCHAR(50) NOT NULL DEFAULT 'metrics-events',
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL,
    UNIQUE KEY uk_aggregate_event (aggregate_id, event_type, created_at),
    KEY idx_published_created (published, created_at),
    KEY idx_topic (topic)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create event_handled table for idempotent processing
CREATE TABLE event_handled (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dedupe_key VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add sales_count column to product_metrics table
ALTER TABLE product_metrics ADD COLUMN sales_count BIGINT DEFAULT 0 AFTER view_count;
