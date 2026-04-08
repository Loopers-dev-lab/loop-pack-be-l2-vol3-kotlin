package com.loopers.application.queue

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class QueueExperimentPropertiesTest {
    @Test
    fun `기본_처리량_설정으로_배치크기를_계산한다`() {
        val properties = QueueExperimentProperties(
            scheduler = QueueExperimentProperties.Scheduler(fixedDelay = Duration.ofSeconds(1)),
            avgOrderProcessingTime = Duration.ofSeconds(2),
            dbConnectionPoolSize = 40,
            dbUtilizationRatio = 0.7,
        )

        assertThat(properties.resolvedBatchSize()).isEqualTo(14)
    }

    @Test
    fun `배치크기_오버라이드가_있으면_계산값보다_우선한다`() {
        val properties = QueueExperimentProperties(batchSizeOverride = 9)

        assertThat(properties.resolvedBatchSize()).isEqualTo(9)
    }
}
