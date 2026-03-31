CREATE TABLE user_action_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT       NOT NULL,
    action_type VARCHAR(20)  NOT NULL,
    target_type VARCHAR(20)  NOT NULL,
    target_id   BIGINT       NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    INDEX idx_ual_member_created (member_id, created_at),
    INDEX idx_ual_target (target_type, target_id, created_at)
);
