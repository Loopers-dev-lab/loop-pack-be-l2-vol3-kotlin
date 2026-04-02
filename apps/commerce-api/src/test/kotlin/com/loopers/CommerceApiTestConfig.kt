package com.loopers

import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate

@org.springframework.context.annotation.Configuration
class CommerceApiTestConfig {
    @Bean("anyKafkaTemplate")
    fun kafkaTemplateAny(): KafkaTemplate<String, Any> {
        return Mockito.mock(KafkaTemplate::class.java) as KafkaTemplate<String, Any>
    }

    @Bean
    @Primary
    fun kafkaTemplate(): KafkaTemplate<String, String> {
        return Mockito.mock(KafkaTemplate::class.java) as KafkaTemplate<String, String>
    }
}
