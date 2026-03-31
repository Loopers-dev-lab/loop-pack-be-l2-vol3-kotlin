package com.loopers.testcontainers

import org.springframework.context.annotation.Configuration
import org.testcontainers.kafka.KafkaContainer

@Configuration
class KafkaTestContainersConfig {
    companion object {
        private val kafkaContainer = KafkaContainer("apache/kafka-native:3.8.0")
            .apply {
                start()
            }

        init {
            System.setProperty("spring.kafka.bootstrap-servers", kafkaContainer.bootstrapServers)
        }
    }
}
