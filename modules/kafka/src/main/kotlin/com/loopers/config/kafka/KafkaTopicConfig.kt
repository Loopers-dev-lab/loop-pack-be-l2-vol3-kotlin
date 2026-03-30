package com.loopers.config.kafka

import com.loopers.event.KafkaTopics
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig {

    @Bean
    fun catalogEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.CATALOG_EVENTS)
            .partitions(3)
            .replicas(1)
            .build()
    }

    @Bean
    fun orderEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.ORDER_EVENTS)
            .partitions(3)
            .replicas(1)
            .build()
    }

    @Bean
    fun couponIssueRequestsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.COUPON_ISSUE_REQUESTS)
            .partitions(3)
            .replicas(1)
            .build()
    }

    @Bean
    fun couponIssueResultsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.COUPON_ISSUE_RESULTS)
            .partitions(3)
            .replicas(1)
            .build()
    }
}
