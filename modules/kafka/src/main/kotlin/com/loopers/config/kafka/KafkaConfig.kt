package com.loopers.config.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
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
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.util.backoff.FixedBackOff

@EnableKafka
@Configuration
class KafkaConfig {
    companion object {
        const val SINGLE_LISTENER = "SINGLE_LISTENER_DEFAULT"
        const val BATCH_LISTENER = "BATCH_LISTENER_DEFAULT"

        /**
         * MAX_POLL_INTERVAL_MS(120s) 안에 모든 레코드를 처리해야 한다.
         * 건당 처리: idempotency INSERT(~30ms) + upsertMetrics(~30ms) = ~60ms.
         * 120s / 60ms = 2000건이 이론적 한계, 75% 안전 마진 적용 → 1500건.
         */
        private const val MAX_POLLING_SIZE = 1500
        private const val FETCH_MIN_BYTES = 1024 * 1024 // 1MB — 소량 fetch 억제, 배치 효율 확보
        private const val FETCH_MAX_WAIT_MS = 5 * 1000 // 5s — FETCH_MIN_BYTES 미달 시 최대 대기
        private const val SESSION_TIMEOUT_MS = 60 * 1000 // 60s
        private const val HEARTBEAT_INTERVAL_MS = 20 * 1000 // 20s (session_timeout의 1/3)
        private const val MAX_POLL_INTERVAL_MS = 2 * 60 * 1000 // 120s
    }

    @Bean
    fun producerFactory(kafkaProperties: KafkaProperties): ProducerFactory<Any, Any> {
        return DefaultKafkaProducerFactory(HashMap(kafkaProperties.buildProducerProperties()))
    }

    @Bean
    fun consumerFactory(kafkaProperties: KafkaProperties): ConsumerFactory<Any, Any> {
        return DefaultKafkaConsumerFactory(HashMap(kafkaProperties.buildConsumerProperties()))
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<Any, Any>): KafkaTemplate<Any, Any> {
        return KafkaTemplate(producerFactory)
    }

    @Bean
    fun kafkaErrorHandler(kafkaTemplate: KafkaTemplate<Any, Any>): DefaultErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate)
        return DefaultErrorHandler(recoverer, FixedBackOff(1000, 3))
    }

    @Bean(SINGLE_LISTENER)
    fun defaultSingleListenerContainerFactory(
        kafkaProperties: KafkaProperties,
        kafkaErrorHandler: DefaultErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<*, *> {
        val consumerConfig = HashMap(kafkaProperties.buildConsumerProperties()).apply {
            put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, SESSION_TIMEOUT_MS)
            put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, HEARTBEAT_INTERVAL_MS)
            put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, MAX_POLL_INTERVAL_MS)
        }

        return ConcurrentKafkaListenerContainerFactory<Any, Any>().apply {
            consumerFactory = DefaultKafkaConsumerFactory(consumerConfig)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            setCommonErrorHandler(kafkaErrorHandler)
            setConcurrency(1)
            isBatchListener = false
        }
    }

    @Bean(BATCH_LISTENER)
    fun defaultBatchListenerContainerFactory(
        kafkaProperties: KafkaProperties,
    ): ConcurrentKafkaListenerContainerFactory<*, *> {
        val originalConfig = HashMap(kafkaProperties.buildConsumerProperties())
        val originalValueDeserializer = originalConfig[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG]

        val consumerConfig = originalConfig.apply {
            put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, MAX_POLLING_SIZE)
            put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, FETCH_MIN_BYTES)
            put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, FETCH_MAX_WAIT_MS)
            put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, SESSION_TIMEOUT_MS)
            put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, HEARTBEAT_INTERVAL_MS)
            put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, MAX_POLL_INTERVAL_MS)
            // [설계 결정] deserialization 실패 시 무한 재시도를 방지한다.
            // ErrorHandlingDeserializer가 실패한 레코드를 null로 전달하여 offset이 전진할 수 있게 한다.
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer::class.java)
            put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer::class.java)
            put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, originalValueDeserializer)
        }

        return ConcurrentKafkaListenerContainerFactory<Any, Any>().apply {
            consumerFactory = DefaultKafkaConsumerFactory(consumerConfig)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            // catalog-events, order-events 토픽 각 3 partition 기준.
            // partition 수 = concurrency일 때 최대 병렬 소비. 초과 시 유휴 스레드 발생.
            setConcurrency(3)
            // [설계 결정] batch factory에는 DefaultErrorHandler를 적용하지 않는다.
            // DefaultErrorHandler는 record-level handler라 batch 전체가 DLT 대상이 된다.
            // 대신 consumer 내부 for-loop + try-catch로 건별 에러를 처리하고,
            // deserialization 실패는 ErrorHandlingDeserializer가 null로 전달하여 처리한다.
            isBatchListener = true
        }
    }
}
