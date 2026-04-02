package com.loopers.config.kafka

import org.apache.kafka.clients.producer.ProducerConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.kafka.core.DefaultKafkaProducerFactory

@DisplayName("KafkaConfig")
class KafkaConfigTest {
    private val kafkaConfig = KafkaConfig()

    @DisplayName("producer 설정은 acks=all, idempotence=true 를 강제한다")
    @Test
    fun producerFactoryForcesReliabilityOptions() {
        // arrange
        val kafkaProperties = KafkaProperties().apply {
            bootstrapServers = listOf("localhost:9092")
        }

        // act
        val producerFactory = kafkaConfig.producerFactory(kafkaProperties) as DefaultKafkaProducerFactory<Any, Any>

        // assert
        assertThat(producerFactory.configurationProperties[ProducerConfig.ACKS_CONFIG]).isEqualTo("all")
        assertThat(producerFactory.configurationProperties[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG]).isEqualTo(true)
    }
}
