package com.loopers.testcontainers

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName

@Configuration
@ConditionalOnProperty(name = ["test.kafka.testcontainers.enabled"], havingValue = "true", matchIfMissing = false)
class KafkaTestContainersConfig {
    companion object {
        private val kafkaContainer: KafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka:3.8.1"))
            .apply {
                start()
            }

        init {
            System.setProperty("spring.kafka.bootstrap-servers", kafkaContainer.bootstrapServers)
        }
    }
}
