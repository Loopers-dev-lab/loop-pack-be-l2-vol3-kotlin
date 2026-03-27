package com.loopers.config.kafka

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
import org.springframework.util.backoff.FixedBackOff
import java.util.HashMap

@EnableKafka
@Configuration
class KafkaConfig {
    companion object {
        const val BATCH_LISTENER = "BATCH_LISTENER_DEFAULT"
        const val RECORD_LISTENER = "RECORD_LISTENER_DEFAULT"

        private const val MAX_POLLING_SIZE = 3000 // read 3000 msg
        private const val FETCH_MIN_BYTES = (1024 * 1024) // 1mb
        private const val FETCH_MAX_WAIT_MS = 5 * 1000 // broker waiting time = 5s
        private const val SESSION_TIMEOUT_MS = 60 * 1000 // session timeout = 1m
        private const val HEARTBEAT_INTERVAL_MS = 20 * 1000 // heartbeat interval = 20s ( 1/3 of session_timeout )
        private const val MAX_POLL_INTERVAL_MS = 2 * 60 * 1000 // max poll interval = 2m

        private const val DLQ_RETRY_COUNT = 3L
        private const val DLQ_RETRY_INTERVAL_MS = 1000L
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

    /**
     * 배치 리스너 컨테이너 팩토리.
     * 대량 메시지를 한번에 poll하여 건별 처리 후 배치 단위 ACK.
     * 용도: 메트릭 집계 등 처리량이 중요한 Consumer.
     */
    @Bean(BATCH_LISTENER)
    fun defaultBatchListenerContainerFactory(
        kafkaProperties: KafkaProperties,
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
            setConcurrency(3)
            isBatchListener = true
        }
    }

    /**
     * 레코드 리스너 컨테이너 팩토리.
     * 메시지 1건씩 처리하며, 실패 시 3회 재시도 후 DLQ로 격리.
     * concurrency=1로 파티션 내 순서 보장.
     * 용도: 선착순 쿠폰 발급 등 순서 보장 + 건별 에러 핸들링이 필요한 Consumer.
     */
    @Bean(RECORD_LISTENER)
    fun defaultRecordListenerContainerFactory(
        kafkaProperties: KafkaProperties,
        kafkaTemplate: KafkaTemplate<Any, Any>,
    ): ConcurrentKafkaListenerContainerFactory<*, *> {
        val consumerConfig = HashMap(kafkaProperties.buildConsumerProperties())
            .apply {
                put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)
                put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, SESSION_TIMEOUT_MS)
                put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, HEARTBEAT_INTERVAL_MS)
                put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, MAX_POLL_INTERVAL_MS)
            }

        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate)
        val errorHandler = DefaultErrorHandler(recoverer, FixedBackOff(DLQ_RETRY_INTERVAL_MS, DLQ_RETRY_COUNT))

        return ConcurrentKafkaListenerContainerFactory<Any, Any>().apply {
            consumerFactory = DefaultKafkaConsumerFactory(consumerConfig)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            setConcurrency(1)
            isBatchListener = false
            setCommonErrorHandler(errorHandler)
        }
    }
}
