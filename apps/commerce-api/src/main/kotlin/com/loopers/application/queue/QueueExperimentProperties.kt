package com.loopers.application.queue

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration
import kotlin.math.floor
import kotlin.math.max

@ConfigurationProperties(prefix = "queue.experiment")
data class QueueExperimentProperties(
    val enabled: Boolean = true,
    val enforceOrderGate: Boolean = false,
    val activeStrategy: QueueStrategyType = QueueStrategyType.REDIS_ONLY,
    val scheduler: Scheduler = Scheduler(),
    val tokenTtl: Duration = Duration.ofMinutes(5),
    val avgOrderProcessingTime: Duration = Duration.ofSeconds(2),
    val dbConnectionPoolSize: Int = 40,
    val dbUtilizationRatio: Double = 0.7,
    val batchSizeOverride: Int? = null,
) {
    fun resolvedBatchSize(): Int {
        batchSizeOverride?.let { return it }

        val perTickCapacity =
            dbConnectionPoolSize * dbUtilizationRatio * scheduler.fixedDelay.toMillis() /
                avgOrderProcessingTime.toMillis().toDouble()
        return max(1, floor(perTickCapacity).toInt())
    }

    data class Scheduler(
        val enabled: Boolean = true,
        val fixedDelay: Duration = Duration.ofSeconds(1),
    )
}
