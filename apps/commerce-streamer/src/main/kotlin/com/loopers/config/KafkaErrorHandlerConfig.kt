package com.loopers.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.converter.StringJsonMessageConverter
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaErrorHandlerConfig {

    companion object {
        const val CATALOG_RETRY_COUNT = 3L
        const val ORDER_RETRY_COUNT = 5L
        const val COUPON_ISSUE_RETRY_COUNT = 3L
        private const val RETRY_INTERVAL_MS = 1000L

        fun resolveDlqDestination(record: ConsumerRecord<*, *>): TopicPartition {
            return TopicPartition("${record.topic()}.dlq", record.partition())
        }
    }

    @Bean("catalogListenerContainerFactory")
    fun catalogListenerContainerFactory(
        consumerFactory: ConsumerFactory<Any, Any>,
        kafkaTemplate: KafkaTemplate<Any, Any>,
        objectMapper: ObjectMapper,
    ): ConcurrentKafkaListenerContainerFactory<Any, Any> {
        return createFactory(consumerFactory, kafkaTemplate, objectMapper, CATALOG_RETRY_COUNT)
    }

    @Bean("couponIssueListenerContainerFactory")
    fun couponIssueListenerContainerFactory(
        consumerFactory: ConsumerFactory<Any, Any>,
        kafkaTemplate: KafkaTemplate<Any, Any>,
        objectMapper: ObjectMapper,
    ): ConcurrentKafkaListenerContainerFactory<Any, Any> {
        return createFactory(consumerFactory, kafkaTemplate, objectMapper, COUPON_ISSUE_RETRY_COUNT)
    }

    @Bean("orderListenerContainerFactory")
    fun orderListenerContainerFactory(
        consumerFactory: ConsumerFactory<Any, Any>,
        kafkaTemplate: KafkaTemplate<Any, Any>,
        objectMapper: ObjectMapper,
    ): ConcurrentKafkaListenerContainerFactory<Any, Any> {
        return createFactory(consumerFactory, kafkaTemplate, objectMapper, ORDER_RETRY_COUNT)
    }

    private fun createFactory(
        consumerFactory: ConsumerFactory<Any, Any>,
        kafkaTemplate: KafkaTemplate<Any, Any>,
        objectMapper: ObjectMapper,
        retryCount: Long,
    ): ConcurrentKafkaListenerContainerFactory<Any, Any> {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate) { record, _ ->
            resolveDlqDestination(record)
        }
        val errorHandler = DefaultErrorHandler(recoverer, FixedBackOff(RETRY_INTERVAL_MS, retryCount))

        return ConcurrentKafkaListenerContainerFactory<Any, Any>().apply {
            this.consumerFactory = consumerFactory
            setCommonErrorHandler(errorHandler)
            setRecordMessageConverter(StringJsonMessageConverter(objectMapper))
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        }
    }
}
