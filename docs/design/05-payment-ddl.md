# Payment DDL

```sql
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    transaction_key VARCHAR(100) UNIQUE,
    card_type VARCHAR(20) NOT NULL,
    card_no VARCHAR(30) NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    INDEX idx_payments_order_id (order_id),
    INDEX idx_payments_status_created_at (status, created_at)
);
```
