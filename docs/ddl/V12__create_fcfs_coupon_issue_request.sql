CREATE TABLE fcfs_coupon_issue_request (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id   BIGINT      NOT NULL,
    member_id     BIGINT      NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at    DATETIME(6) NOT NULL,
    processed_at  DATETIME(6) NULL,
    INDEX idx_fcfs_req_template_member (template_id, member_id),
    INDEX idx_fcfs_req_status (status, created_at)
);
