package com.loopers

import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate

@org.springframework.context.annotation.Configuration
class CommerceApiTestConfig {
    @Bean
    @Primary
    fun kafkaTemplate(): KafkaTemplate<String, Any> {
        return Mockito.mock(KafkaTemplate::class.java) as KafkaTemplate<String, Any>
    }
}
