package com.loopers.config.kafka

object KafkaTopics {
    const val CATALOG_EVENTS = "catalog-events"
    const val ORDER_EVENTS = "order-events"
    const val COUPON_ISSUE_REQUESTS = "coupon-issue-requests"

    const val GROUP_CATALOG_METRICS = "streamer-catalog-metrics"
    const val GROUP_ORDER_METRICS = "streamer-order-metrics"
    const val GROUP_COUPON_ISSUE = "streamer-coupon-issue"
}
