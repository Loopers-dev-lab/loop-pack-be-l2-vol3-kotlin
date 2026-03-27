CREATE TABLE IF NOT EXISTS dlq_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_topic VARCHAR(255) NOT NULL,
    message_payload LONGTEXT NOT NULL,
    consumer_group VARCHAR(255) NOT NULL,
    event_type VARCHAR(255),
    error_message TEXT,
    error_stack_trace LONGTEXT,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    last_retry_at DATETIME,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    INDEX idx_status (status),
    INDEX idx_original_topic (original_topic),
    INDEX idx_consumer_group (consumer_group)
);
