package com.loopers.application.queue

enum class QueueStrategyType {
    REDIS_ONLY,
    REDIS_KAFKA,
    KAFKA_ONLY,
    PESSIMISTIC_LOCK,
    DISTRIBUTED_LOCK,
}
