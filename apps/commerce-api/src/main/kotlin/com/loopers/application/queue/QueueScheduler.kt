package com.loopers.application.queue

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class QueueScheduler(
    private val queueExperimentProperties: QueueExperimentProperties,
    private val queueExperimentUseCase: QueueExperimentUseCase,
) {
    private val log = LoggerFactory.getLogger(QueueScheduler::class.java)

    @Scheduled(fixedDelayString = "\${queue.experiment.scheduler.fixed-delay:PT1S}")
    fun admitWaitingMembers() {
        if (!queueExperimentProperties.enabled || !queueExperimentProperties.scheduler.enabled) {
            return
        }

        val strategyType = queueExperimentProperties.activeStrategy
        val admittedCount = queueExperimentUseCase.admitWaiting(strategyType)
        if (admittedCount > 0) {
            log.info(
                "queue admission completed strategy={} batchSize={} admittedCount={}",
                strategyType,
                queueExperimentUseCase.resolvedBatchSize(),
                admittedCount,
            )
        }
    }
}
