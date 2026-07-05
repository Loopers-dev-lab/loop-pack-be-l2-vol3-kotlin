package com.loopers.infrastructure.queue

import com.loopers.application.queue.EnterQueueUseCase
import com.loopers.application.queue.GetQueuePositionUseCase
import com.loopers.application.queue.QueueEntryScheduler
import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.domain.queue.QueueThroughput
import com.loopers.domain.queue.WaitingQueueRepository
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 대기열 통합 테스트.
 * 백그라운드 스케줄러(100ms 폴링)는 테스트 프로파일 전역에서 꺼져 있다
 * (src/test/resources/application-test.yml). Redis 컨테이너가 JVM 전체 공유라
 * 이 테스트만 꺼서는 캐시된 다른 컨텍스트의 스케줄러가 대기열을 드레인한다.
 * 스케줄러 동작 자체는 수동 생성한 인스턴스로 검증한다.
 */
@SpringBootTest
@Import(MySqlTestContainersConfig::class, RedisTestContainersConfig::class)
class QueueRedisIntegrationTest {

    @Autowired
    private lateinit var waitingQueueRepository: WaitingQueueRepository

    @Autowired
    private lateinit var entryTokenRepository: EntryTokenRepository

    @Autowired
    private lateinit var enterQueueUseCase: EnterQueueUseCase

    @Autowired
    private lateinit var getQueuePositionUseCase: GetQueuePositionUseCase

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Autowired
    private lateinit var redisCleanUp: RedisCleanUp

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    inner class ConcurrentEnter {

        @Test
        fun `동시에 진입한 모든 유저가 유실 없이 고유한 순번을 부여받는다`() {
            val userCount = 50

            runConcurrently(userCount) { i -> enterQueueUseCase.enter(i + 1L) }

            assertThat(waitingQueueRepository.getQueueSize()).isEqualTo(userCount.toLong())

            val positions = (1..userCount.toLong()).map { userId ->
                getQueuePositionUseCase.getPosition(userId).position
            }
            assertThat(positions).containsExactlyInAnyOrderElementsOf((1..userCount.toLong()).toList())
        }

        @Test
        fun `같은 유저가 동시에 중복 진입해도 대기열에는 한 번만 등록된다`() {
            runConcurrently(20) { enterQueueUseCase.enter(USER_ID) }

            assertThat(waitingQueueRepository.getQueueSize()).isEqualTo(1)
        }

        @Test
        fun `먼저 진입한 유저가 먼저 대기열에서 나온다`() {
            waitingQueueRepository.enqueue(30L, 1.0)
            waitingQueueRepository.enqueue(10L, 2.0)
            waitingQueueRepository.enqueue(20L, 3.0)

            val dequeued = waitingQueueRepository.dequeueTopN(3)

            assertThat(dequeued).containsExactly(30L, 10L, 20L)
        }

        @Test
        fun `두 스레드가 동시에 dequeue해도 같은 유저가 중복으로 나오지 않는다`() {
            val userCount = 40L
            for (i in 1..userCount) {
                waitingQueueRepository.enqueue(i, i.toDouble())
            }

            val executor = Executors.newFixedThreadPool(2)
            try {
                val startSignal = CountDownLatch(1)
                val futures = listOf(20L, 20L).map { count ->
                    executor.submit<List<Long>> {
                        startSignal.await()
                        waitingQueueRepository.dequeueTopN(count)
                    }
                }
                startSignal.countDown()
                val first = futures[0].get(10, TimeUnit.SECONDS)
                val second = futures[1].get(10, TimeUnit.SECONDS)

                assertThat(first + second).containsExactlyInAnyOrderElementsOf((1..userCount).toList())
            } finally {
                executor.shutdownNow()
            }
        }
    }

    @Nested
    inner class TokenExpiry {

        @Test
        fun `발급된 토큰에는 TTL이 설정되어 있다`() {
            entryTokenRepository.issueToken(USER_ID, TOKEN, ttlSeconds = 300)

            val expire = redisTemplate.getExpire("queue:order:token:$USER_ID", TimeUnit.SECONDS)

            assertThat(expire).isBetween(1L, 300L)
        }

        @Test
        fun `TTL이 지난 토큰은 자동 만료되어 무효화된다`() {
            entryTokenRepository.issueToken(USER_ID, TOKEN, ttlSeconds = 2)
            assertThat(entryTokenRepository.findToken(USER_ID)).isEqualTo(TOKEN)

            awaitTokenExpiry(USER_ID)

            assertThat(entryTokenRepository.findToken(USER_ID)).isNull()
            assertThat(entryTokenRepository.hasToken(USER_ID)).isFalse()
        }

        @Test
        fun `토큰이 만료된 유저는 순번 조회 시 토큰 없는 상태로 처리된다`() {
            entryTokenRepository.issueToken(USER_ID, TOKEN, ttlSeconds = 2)

            awaitTokenExpiry(USER_ID)

            val result = getQueuePositionUseCase.getPosition(USER_ID)
            assertThat(result.token).isNull()
        }

        /** 고정 sleep 대신 만료를 폴링 대기한다. 느린 CI에서도 flaky하지 않다. */
        private fun awaitTokenExpiry(userId: Long, timeoutMs: Long = 5_000) {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (entryTokenRepository.findToken(userId) != null && System.currentTimeMillis() < deadline) {
                Thread.sleep(50)
            }
        }
    }

    @Nested
    inner class SchedulerThroughput {

        @Test
        fun `배치 크기를 초과하는 대기 인원이 있어도 한 번에 배치 크기만큼만 입장시킨다`() {
            val scheduler = QueueEntryScheduler(waitingQueueRepository, entryTokenRepository)
            val overflow = 11L
            val userCount = QueueThroughput.BATCH_SIZE + overflow
            for (i in 1..userCount) {
                waitingQueueRepository.enqueue(i, i.toDouble())
            }

            scheduler.processQueue()

            assertThat(waitingQueueRepository.getQueueSize()).isEqualTo(overflow)
            for (i in 1..QueueThroughput.BATCH_SIZE) {
                assertThat(entryTokenRepository.hasToken(i)).isTrue()
            }
            for (i in (QueueThroughput.BATCH_SIZE + 1)..userCount) {
                assertThat(entryTokenRepository.hasToken(i)).isFalse()
            }
        }

        @Test
        fun `스케줄러가 반복 실행되면 대기열이 모두 소진되고 전원에게 토큰이 발급된다`() {
            val scheduler = QueueEntryScheduler(waitingQueueRepository, entryTokenRepository)
            val userCount = QueueThroughput.BATCH_SIZE + 11
            for (i in 1..userCount) {
                waitingQueueRepository.enqueue(i, i.toDouble())
            }

            var cycles = 0
            while (waitingQueueRepository.getQueueSize() > 0 && cycles < 10) {
                scheduler.processQueue()
                cycles++
            }

            assertThat(waitingQueueRepository.getQueueSize()).isZero()
            for (i in 1..userCount) {
                assertThat(entryTokenRepository.hasToken(i)).isTrue()
            }
        }
    }

    /**
     * taskCount개의 작업을 동시에 시작시키고 완료를 대기한다.
     * 단언 실패나 타임아웃에도 executor가 누수되지 않도록 finally로 정리한다.
     */
    private fun runConcurrently(taskCount: Int, task: (Int) -> Unit) {
        val executor = Executors.newFixedThreadPool(minOf(taskCount, 32))
        try {
            val startSignal = CountDownLatch(1)
            val doneSignal = CountDownLatch(taskCount)
            repeat(taskCount) { i ->
                executor.submit {
                    startSignal.await()
                    try {
                        task(i)
                    } finally {
                        doneSignal.countDown()
                    }
                }
            }
            startSignal.countDown()
            assertThat(doneSignal.await(10, TimeUnit.SECONDS)).isTrue()
        } finally {
            executor.shutdownNow()
        }
    }

    companion object {
        private const val USER_ID = 1L
        private const val TOKEN = "test-token"
    }
}
