package com.loopers.domain.queue

import com.loopers.support.error.CoreException
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class OrderQueueServiceConcurrencyTest @Autowired constructor(
    private val orderQueueService: OrderQueueService,
    private val entryTokenRepository: EntryTokenRepository,
    private val redisCleanUp: RedisCleanUp,
) {

    @MockitoBean
    private lateinit var queueHealthChecker: QueueHealthChecker

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Test
    @DisplayName("동일한 토큰으로 동시 요청을 보내면 정확히 1건만 성공한다")
    fun onlyOneRequestSucceeds_whenConcurrentRequestsWithSameToken() {
        // arrange
        val userId = 1L
        val token = UUID.randomUUID().toString()
        whenever(queueHealthChecker.isBypassed()).thenReturn(false)
        entryTokenRepository.issue(userId, token, 300L)

        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // act
        repeat(threadCount) {
            executor.submit {
                try {
                    orderQueueService.validateAndConsumeToken(userId, token)
                    successCount.incrementAndGet()
                } catch (e: CoreException) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executor.shutdown()

        // assert — 정확히 1건만 성공
        assertThat(successCount.get()).isEqualTo(1)
        assertThat(failCount.get()).isEqualTo(threadCount - 1)
    }
}
