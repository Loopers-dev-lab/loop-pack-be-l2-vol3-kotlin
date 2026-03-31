package com.loopers.infrastructure.outbox

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate

@Configuration
class KafkaOutboxRelayTemplateConfig {
    @Bean
    fun kafkaOutboxRelayTemplate(
        kafkaTemplate: KafkaTemplate<String, Any>,
    ): KafkaTemplate<String, KafkaOutboxEnvelope> {
        @Suppress("UNCHECKED_CAST")
        return kafkaTemplate as KafkaTemplate<String, KafkaOutboxEnvelope>
    }
}
