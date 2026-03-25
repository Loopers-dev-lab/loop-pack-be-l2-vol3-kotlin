package com.loopers.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.event.EventEnvelope
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.kafka.test.utils.KafkaTestUtils

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = ["catalog-events", "catalog-events.dlq", "order-events", "order-events.dlq"],
    bootstrapServersProperty = "spring.kafka.bootstrap-servers",
)
abstract class EmbeddedKafkaTestSupport {

    @Autowired
    protected lateinit var embeddedKafka: EmbeddedKafkaBroker

    @Autowired
    protected lateinit var kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    protected fun waitForConsumerAssignment() {
        kafkaListenerEndpointRegistry.listenerContainers.forEach { container ->
            ContainerTestUtils.waitForAssignment(container, embeddedKafka.partitionsPerTopic)
        }
    }

    protected fun sendEnvelope(topic: String, envelope: EventEnvelope) {
        sendStringMessage(topic, envelope.aggregateId, objectMapper.writeValueAsString(envelope))
    }

    protected fun sendStringMessage(topic: String, key: String, value: String) {
        val props = KafkaTestUtils.producerProps(embeddedKafka).toMutableMap()
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        DefaultKafkaProducerFactory<String, String>(props).createProducer().use { producer ->
            producer.send(ProducerRecord(topic, key, value)).get()
        }
    }

    protected fun createStringConsumer(groupId: String): org.apache.kafka.clients.consumer.Consumer<String, String> {
        val props = KafkaTestUtils.consumerProps(groupId, "true", embeddedKafka).toMutableMap()
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        return DefaultKafkaConsumerFactory<String, String>(props).createConsumer()
    }
}
