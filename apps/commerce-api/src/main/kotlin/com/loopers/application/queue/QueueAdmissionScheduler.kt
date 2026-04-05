package com.loopers.application.queue

import com.loopers.domain.queue.OrderQueueService
import com.loopers.domain.queue.QueueHealthChecker
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

@Profile("!test")
@Component
class QueueAdmissionScheduler(
    private val orderQueueService: OrderQueueService,
    private val queueFacade: QueueFacade,
    private val queueHealthChecker: QueueHealthChecker,
    @Value("\${queue.admission.batch-size}") private val batchSize: Long,
    @Value("\${queue.admission.fixed-rate}") private val fixedRate: Long,
    @Value("\${queue.admission.jitter-range}") private val jitterRange: Long,
) : SmartLifecycle {

    private val log = LoggerFactory.getLogger(javaClass)
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    @Volatile
    private var running = false

    @Volatile
    private var wasBypassed = false

    fun admitUsers() {
        try {
            if (queueHealthChecker.isBypassed()) {
                if (!wasBypassed) {
                    log.warn("대기열 bypass 상태 감지 — SSE 구독자에게 bypass 알림 전송")
                    queueFacade.broadcastBypass()
                    wasBypassed = true
                }
                return
            }
            wasBypassed = false
            val admittedUsers = orderQueueService.admitUsers(batchSize)
            if (admittedUsers.isNotEmpty()) {
                log.info("대기열 입장 허용: {}명", admittedUsers.size)
            }
            queueFacade.broadcastPositions(admittedUsers)
        } catch (e: Exception) {
            log.warn("대기열 입장 허용 중 오류 발생", e)
        } finally {
            scheduleNext()
        }
    }

    fun calculateNextDelay(): Long {
        if (jitterRange <= 0) return fixedRate
        val jitter = ThreadLocalRandom.current().nextLong(-jitterRange, jitterRange + 1)
        return fixedRate + jitter
    }

    private fun scheduleNext() {
        if (running) {
            executor.schedule(::admitUsers, calculateNextDelay(), TimeUnit.MILLISECONDS)
        }
    }

    override fun start() {
        running = true
        scheduleNext()
    }

    override fun stop() {
        running = false
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    override fun isRunning(): Boolean = running
}
