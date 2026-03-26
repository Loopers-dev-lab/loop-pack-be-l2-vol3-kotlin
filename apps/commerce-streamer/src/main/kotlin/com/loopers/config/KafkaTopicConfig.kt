package com.loopers.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KafkaTopicConfig {

    @Bean
    fun catalogEventsTopic(): NewTopic = NewTopic("catalog-events", 3, 1.toShort())

    @Bean
    fun orderEventsTopic(): NewTopic = NewTopic("order-events", 3, 1.toShort())
}
