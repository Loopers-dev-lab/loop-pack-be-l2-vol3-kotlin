package com.loopers.infrastructure.queue

import com.loopers.domain.queue.QueueProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * QueueProperties 생성자 바인딩 설정.
 *
 * @Bean + @ConfigurationProperties 팩토리 메서드 방식은 setter 바인딩을 사용하므로
 * val(불변) 프로퍼티만 있는 data class에서 기본값과 다른 yml 값을 주입할 수 없다.
 * @Value로 각 프로퍼티를 직접 주입해 생성자로 QueueProperties를 생성하는 방식으로 해결.
 */
@Configuration
class QueuePropertiesConfig {

    @Bean
    fun queueProperties(
        @Value("\${queue.max-queue-size:50000}") maxQueueSize: Long,
        @Value("\${queue.token-ttl-seconds:300}") tokenTtlSeconds: Long,
        @Value("\${queue.batch-size:18}") batchSize: Int,
        @Value("\${queue.max-active-tokens:175}") maxActiveTokens: Long,
        @Value("\${queue.throughput-per-second:175.0}") throughputPerSecond: Double,
        @Value("\${queue.scheduler.enabled:true}") schedulerEnabled: Boolean,
        @Value("\${queue.processing-timeout-seconds:30}") processingTimeoutSeconds: Long,
    ): QueueProperties = QueueProperties(
        maxQueueSize = maxQueueSize,
        tokenTtlSeconds = tokenTtlSeconds,
        batchSize = batchSize,
        maxActiveTokens = maxActiveTokens,
        throughputPerSecond = throughputPerSecond,
        scheduler = QueueProperties.Scheduler(enabled = schedulerEnabled),
        processingTimeoutSeconds = processingTimeoutSeconds,
    )
}
