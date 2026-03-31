package com.loopers.config.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.clients.producer.ProducerConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.converter.ByteArrayJsonMessageConverter

@DisplayName("KafkaConfig")
class KafkaConfigTest {
    private val kafkaConfig = KafkaConfig()

    @Test
    @DisplayName("producer는 acks=all과 idempotence=true를 사용한다")
    fun producerFactory_hasIdempotenceSettings() {
        val producerFactory = kafkaConfig.producerFactory(KafkaProperties())

        assertThat(producerFactory.configurationProperties[ProducerConfig.ACKS_CONFIG]).isEqualTo("all")
        assertThat(producerFactory.configurationProperties[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG]).isEqualTo(true)
    }

    @Test
    @DisplayName("batch listener는 manual ack와 single-thread concurrency를 사용한다")
    fun listenerFactory_usesManualAck() {
        val listenerFactory = kafkaConfig.defaultBatchListenerContainerFactory(
            KafkaProperties(),
            ByteArrayJsonMessageConverter(jacksonObjectMapper()),
        )
        val concurrencyField = listenerFactory.javaClass.getDeclaredField("concurrency")
        concurrencyField.isAccessible = true

        assertThat(listenerFactory.containerProperties.ackMode).isEqualTo(ContainerProperties.AckMode.MANUAL)
        assertThat(listenerFactory.isBatchListener).isTrue()
        assertThat(concurrencyField.get(listenerFactory)).isEqualTo(1)
    }
}
