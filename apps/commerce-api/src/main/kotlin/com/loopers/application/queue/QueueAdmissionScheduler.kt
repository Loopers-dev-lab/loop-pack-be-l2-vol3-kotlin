package com.loopers.application.queue

import com.loopers.domain.queue.OrderQueueService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Profile("!test")
@Component
class QueueAdmissionScheduler(
    private val orderQueueService: OrderQueueService,
    @Value("\${queue.admission.batch-size}") private val batchSize: Long,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRateString = "\${queue.admission.fixed-rate}")
    fun admitUsers() {
        val admitted = orderQueueService.admitUsers(batchSize)
        if (admitted > 0) {
            log.info("대기열 입장 허용: {}명", admitted)
        }
    }
}
