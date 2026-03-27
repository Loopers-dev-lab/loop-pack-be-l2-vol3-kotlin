package com.loopers.infrastructure.outbox

enum class KafkaEventType {
    PRODUCT_DETAIL_VIEWED,
    PRODUCT_LIKE_REGISTERED,
    PRODUCT_LIKE_CANCELED,
    PAYMENT_SUCCEEDED,
}
