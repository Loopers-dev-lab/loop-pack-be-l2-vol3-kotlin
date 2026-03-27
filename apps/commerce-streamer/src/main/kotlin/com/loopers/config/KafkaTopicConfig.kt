package com.loopers.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KafkaTopicConfig {

    companion object {
        const val CATALOG_EVENTS = "catalog-events"
        const val ORDER_EVENTS = "order-events"
        const val COUPON_ISSUE_REQUESTS = "coupon-issue-requests"

        const val CATALOG_EVENTS_DLT = "$CATALOG_EVENTS.DLT"
        const val ORDER_EVENTS_DLT = "$ORDER_EVENTS.DLT"
        const val COUPON_ISSUE_REQUESTS_DLT = "$COUPON_ISSUE_REQUESTS.DLT"

        fun dlqTopicOf(originalTopic: String): String = "$originalTopic.DLT"
    }

    @Bean
    fun catalogEventsTopic(): NewTopic = NewTopic(CATALOG_EVENTS, 3, 1.toShort())

    @Bean
    fun orderEventsTopic(): NewTopic = NewTopic(ORDER_EVENTS, 3, 1.toShort())

    @Bean
    fun couponIssueRequestsTopic(): NewTopic = NewTopic(COUPON_ISSUE_REQUESTS, 3, 1.toShort())

    @Bean
    fun catalogEventsDltTopic(): NewTopic = NewTopic(CATALOG_EVENTS_DLT, 1, 1.toShort())

    @Bean
    fun orderEventsDltTopic(): NewTopic = NewTopic(ORDER_EVENTS_DLT, 1, 1.toShort())

    @Bean
    fun couponIssueRequestsDltTopic(): NewTopic = NewTopic(COUPON_ISSUE_REQUESTS_DLT, 1, 1.toShort())
}
