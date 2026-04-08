package com.loopers.testcontainers

import org.springframework.context.annotation.Configuration
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

@Configuration
class KafkaTestContainersConfig {
    companion object {
        private val kafkaContainer: KafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .apply {
                start()
            }

        init {
            System.setProperty("spring.kafka.bootstrap-servers", kafkaContainer.bootstrapServers)
        }
    }
}
