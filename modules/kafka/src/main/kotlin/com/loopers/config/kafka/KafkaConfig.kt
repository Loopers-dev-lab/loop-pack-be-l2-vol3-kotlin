package com.loopers.config.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter
import org.springframework.kafka.support.converter.ByteArrayJsonMessageConverter
import java.util.HashMap

@EnableKafka
@Configuration
class KafkaConfig {
    companion object {
        const val BATCH_LISTENER = "BATCH_LISTENER_DEFAULT"

        private const val MAX_POLLING_SIZE = 3000 // read 3000 msg
        private const val FETCH_MIN_BYTES = (1024 * 1024) // 1mb
        private const val FETCH_MAX_WAIT_MS = 5 * 1000 // broker waiting time = 5s
        private const val SESSION_TIMEOUT_MS = 60 * 1000 // session timeout = 1m
        private const val HEARTBEAT_INTERVAL_MS = 20 * 1000 // heartbeat interval = 20s ( 1/3 of session_timeout )
        private const val MAX_POLL_INTERVAL_MS = 2 * 60 * 1000 // max poll interval = 2m
    }

    @Bean
    fun producerFactory(
        kafkaProperties: KafkaProperties,
    ): ProducerFactory<Any, Any> {
        val props: Map<String, Any> = HashMap(kafkaProperties.buildProducerProperties())
        return DefaultKafkaProducerFactory(props)
    }

    @Bean
    fun consumerFactory(
        kafkaProperties: KafkaProperties,
    ): ConsumerFactory<Any, Any> {
        val props: Map<String, Any> = HashMap(kafkaProperties.buildConsumerProperties())
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<Any, Any>): KafkaTemplate<Any, Any> {
        return KafkaTemplate(producerFactory)
    }

    @Bean
    fun jsonMessageConverter(objectMapper: ObjectMapper): ByteArrayJsonMessageConverter {
        return ByteArrayJsonMessageConverter(objectMapper)
    }

    /**
     * Batch 리스너 컨테이너 — manual batch ack 전략.
     *
     * Consumer는 forEach로 레코드별 처리 후 배치 끝에서 `acknowledgment.acknowledge()`로 한 번에 ACK한다.
     * 중복은 EventHandled 테이블의 eventId 기반 멱등 레이어가 방어하며, 처리 실패 건은
     * RetryableRecordProcessor 재시도 후 DLQ로 격리한다.
     *
     * 단건 manual ack이 더 엄격하긴 하지만 ACK 오버헤드가 poll 크기 배수로 커지고,
     * 이미 재시도와 DLQ, 멱등 레이어가 있어 배치 ack에서 추가로 얻는 안전 이득이 크지 않다.
     * Auto commit은 poll 시점에 자동 ACK되어 처리 실패 건이 유실될 위험 때문에 배제.
     */
    @Bean(BATCH_LISTENER)
    fun defaultBatchListenerContainerFactory(
        kafkaProperties: KafkaProperties,
        converter: ByteArrayJsonMessageConverter,
    ): ConcurrentKafkaListenerContainerFactory<*, *> {
        val consumerConfig = HashMap(kafkaProperties.buildConsumerProperties())
            .apply {
                put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, MAX_POLLING_SIZE)
                put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, FETCH_MIN_BYTES)
                put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, FETCH_MAX_WAIT_MS)
                put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, SESSION_TIMEOUT_MS)
                put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, HEARTBEAT_INTERVAL_MS)
                put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, MAX_POLL_INTERVAL_MS)
            }

        return ConcurrentKafkaListenerContainerFactory<Any, Any>().apply {
            consumerFactory = DefaultKafkaConsumerFactory(consumerConfig)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            setBatchMessageConverter(BatchMessagingMessageConverter(converter))
            setConcurrency(3)
            isBatchListener = true
        }
    }
}
