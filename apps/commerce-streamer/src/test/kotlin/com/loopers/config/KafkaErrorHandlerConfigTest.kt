package com.loopers.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties

@ExtendWith(MockitoExtension::class)
@DisplayName("KafkaErrorHandlerConfig")
class KafkaErrorHandlerConfigTest {

    @Mock
    private lateinit var consumerFactory: ConsumerFactory<Any, Any>

    @Mock
    private lateinit var kafkaTemplate: KafkaTemplate<Any, Any>

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @DisplayName("DLQ 목적지 결정 시,")
    @Nested
    inner class DlqDestination {

        @DisplayName("catalog-events 토픽은 catalog-events.dlq로 라우팅한다.")
        @Test
        fun routesCatalogEventsToDlq() {
            // arrange
            val record = ConsumerRecord<String, String>("catalog-events", 0, 0L, "key", "value")

            // act
            val destination = KafkaErrorHandlerConfig.resolveDlqDestination(record)

            // assert
            assertThat(destination.topic()).isEqualTo("catalog-events.dlq")
            assertThat(destination.partition()).isEqualTo(0)
        }

        @DisplayName("order-events 토픽은 order-events.dlq로 라우팅한다.")
        @Test
        fun routesOrderEventsToDlq() {
            // arrange
            val record = ConsumerRecord<String, String>("order-events", 2, 0L, "key", "value")

            // act
            val destination = KafkaErrorHandlerConfig.resolveDlqDestination(record)

            // assert
            assertThat(destination.topic()).isEqualTo("order-events.dlq")
            assertThat(destination.partition()).isEqualTo(2)
        }
    }

    @DisplayName("리스너 컨테이너 팩토리 설정 시,")
    @Nested
    inner class ListenerContainerFactory {

        @DisplayName("catalog 팩토리는 manual ack 모드로 설정된다.")
        @Test
        fun catalogFactoryConfig() {
            // arrange
            val config = KafkaErrorHandlerConfig()

            // act
            val factory = config.catalogListenerContainerFactory(consumerFactory, kafkaTemplate, objectMapper)

            // assert
            assertThat(factory.containerProperties.ackMode)
                .isEqualTo(ContainerProperties.AckMode.MANUAL)
        }

        @DisplayName("order 팩토리는 manual ack 모드로 설정된다.")
        @Test
        fun orderFactoryConfig() {
            // arrange
            val config = KafkaErrorHandlerConfig()

            // act
            val factory = config.orderListenerContainerFactory(consumerFactory, kafkaTemplate, objectMapper)

            // assert
            assertThat(factory.containerProperties.ackMode)
                .isEqualTo(ContainerProperties.AckMode.MANUAL)
        }
    }
}
