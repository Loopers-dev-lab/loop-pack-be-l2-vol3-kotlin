package com.loopers.config.kafka

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig {
    companion object {
        const val PAYMENT_SUCCEEDED_TOPIC = "payment.succeeded"
        const val PAYMENT_FAILED_TOPIC = "payment.failed"
        const val PRODUCT_ACTION_TOPIC = "product.action"
        const val COUPON_ISSUE_REQUEST_TOPIC = "coupon.issue.request"
    }

    @Bean
    fun paymentSucceededTopic(): NewTopic {
        return TopicBuilder.name(PAYMENT_SUCCEEDED_TOPIC)
            .partitions(3)
            .replicas(1)
            .build()
    }

    @Bean
    fun paymentFailedTopic(): NewTopic {
        return TopicBuilder.name(PAYMENT_FAILED_TOPIC)
            .partitions(3)
            .replicas(1)
            .build()
    }

    @Bean
    fun productActionTopic(): NewTopic {
        return TopicBuilder.name(PRODUCT_ACTION_TOPIC)
            .partitions(3)
            .replicas(1)
            .build()
    }

    @Bean
    fun couponIssueRequestTopic(): NewTopic {
        return TopicBuilder.name(COUPON_ISSUE_REQUEST_TOPIC)
            .partitions(3)
            .replicas(1)
            .build()
    }
}
