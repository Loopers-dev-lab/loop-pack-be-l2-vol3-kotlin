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
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter
import org.springframework.kafka.support.converter.ByteArrayJsonMessageConverter
import org.springframework.util.backoff.FixedBackOff
import java.util.HashMap

@EnableKafka
@Configuration
class KafkaConfig {
    companion object {
        const val BATCH_LISTENER = "BATCH_LISTENER_DEFAULT"
        const val RECORD_LISTENER = "RECORD_LISTENER_DEFAULT"
        const val ORDERED_RECORD_LISTENER = "ORDERED_RECORD_LISTENER"

        private const val MAX_POLLING_SIZE = 3000
        private const val FETCH_MIN_BYTES = (1024 * 1024)
        private const val FETCH_MAX_WAIT_MS = 5 * 1000
        private const val SESSION_TIMEOUT_MS = 60 * 1000
        private const val HEARTBEAT_INTERVAL_MS = 20 * 1000
        private const val MAX_POLL_INTERVAL_MS = 2 * 60 * 1000
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
     * DLQ ErrorHandler — 3회 재시도(1초 간격) 후 실패 시 DLQ 토픽으로 격리
     * 원본 토픽명 + ".DLQ" 토픽으로 메시지 이동
     */
    @Bean
    fun kafkaErrorHandler(kafkaTemplate: KafkaTemplate<Any, Any>): DefaultErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate)
        return DefaultErrorHandler(recoverer, FixedBackOff(1000L, 3L))
    }

    /**
     * 건별 처리 리스너 (concurrency=3, 파티션 수와 동일)
     * - 메트릭 집계, 결과 수신 등 순서 보장이 불필요한 Consumer에 사용
     * - 파티션 3개 × 스레드 3개 = 최적 병렬 처리
     * - DLQ 적용: 3회 재시도 후 실패 시 .DLQ 토픽으로 격리
     */
    @Bean(RECORD_LISTENER)
    fun defaultRecordListenerContainerFactory(
        kafkaProperties: KafkaProperties,
        converter: ByteArrayJsonMessageConverter,
        kafkaErrorHandler: DefaultErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<*, *> {
        val consumerConfig = HashMap(kafkaProperties.buildConsumerProperties())
            .apply {
                put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)
                put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30 * 1000)
                put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10 * 1000)
                put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 60 * 1000)
            }

        return ConcurrentKafkaListenerContainerFactory<Any, Any>().apply {
            consumerFactory = DefaultKafkaConsumerFactory(consumerConfig)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            setRecordMessageConverter(converter)
            setCommonErrorHandler(kafkaErrorHandler)
            setConcurrency(3)
            isBatchListener = false
        }
    }

    /**
     * 순서 보장 건별 처리 리스너 (concurrency=1)
     * - 쿠폰 발급 등 같은 파티션 내 순차 처리가 필수인 Consumer에 사용
     * - 단일 스레드로 파티션 순서 보장
     * - DLQ 적용: 3회 재시도 후 실패 시 .DLQ 토픽으로 격리
     */
    @Bean(ORDERED_RECORD_LISTENER)
    fun orderedRecordListenerContainerFactory(
        kafkaProperties: KafkaProperties,
        converter: ByteArrayJsonMessageConverter,
        kafkaErrorHandler: DefaultErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<*, *> {
        val consumerConfig = HashMap(kafkaProperties.buildConsumerProperties())
            .apply {
                put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)
                put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30 * 1000)
                put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10 * 1000)
                put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 60 * 1000)
            }

        return ConcurrentKafkaListenerContainerFactory<Any, Any>().apply {
            consumerFactory = DefaultKafkaConsumerFactory(consumerConfig)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            setRecordMessageConverter(converter)
            setCommonErrorHandler(kafkaErrorHandler)
            setConcurrency(1)
            isBatchListener = false
        }
    }

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
