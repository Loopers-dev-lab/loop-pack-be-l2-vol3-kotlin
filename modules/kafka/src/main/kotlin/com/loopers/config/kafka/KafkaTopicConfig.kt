package com.loopers.config.kafka

import com.loopers.event.EventContract
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig {
    companion object {
        const val PAYMENT_SUCCEEDED_TOPIC = EventContract.PAYMENT_SUCCEEDED_TOPIC
        const val PAYMENT_FAILED_TOPIC = EventContract.PAYMENT_FAILED_TOPIC
        const val PRODUCT_ACTION_TOPIC = EventContract.PRODUCT_ACTION_TOPIC
        const val COUPON_ISSUE_REQUEST_TOPIC = EventContract.COUPON_ISSUE_REQUEST_TOPIC
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
