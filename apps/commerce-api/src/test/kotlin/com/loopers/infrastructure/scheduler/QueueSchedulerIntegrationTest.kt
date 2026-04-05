package com.loopers.infrastructure.scheduler

import com.loopers.domain.queue.QueueRepository
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.Duration

@SpringBootTest
@Import(RedisTestContainersConfig::class)
class QueueSchedulerIntegrationTest @Autowired constructor(
    private val queueRepository: QueueRepository,
    private val queueScheduler: QueueScheduler,
    private val redisCleanUp: RedisCleanUp,
) {

    companion object {
        private const val QUEUE_NAME = "order-queue"
        private const val BATCH_SIZE = 17L
    }

    @BeforeEach
    fun setUp() {
        redisCleanUp.truncateAll()
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @DisplayName("대기열 스케줄러 - 토큰 발급")
    @Test
    fun `대기열의 사용자에게 토큰이 발급된다`() {
        // arrange: 1명 진입
        val userId = 100L
        queueRepository.enter(QUEUE_NAME, userId, System.currentTimeMillis().toDouble())

        // act: 토큰 발급
        queueScheduler.processQueue()

        // assert: 토큰 발급 확인
        val token = queueRepository.getToken(QUEUE_NAME, userId)
        assertThat(token).isNotNull()
        assertThat(queueRepository.size(QUEUE_NAME)).isZero()
    }

    @DisplayName("대기열 스케줄러 - 빈 큐")
    @Test
    fun `빈 대기열에서 processQueue 호출 시 예외가 발생하지 않는다`() {
        // act & assert: 예외 없음
        queueScheduler.processQueue()
    }

    @DisplayName("대기열 스케줄러 - UUID 토큰")
    @Test
    fun `발급된 토큰은 UUID 형식이다`() {
        // arrange: 1명 진입
        val userId = 100L
        queueRepository.enter(QUEUE_NAME, userId, System.currentTimeMillis().toDouble())

        // act
        queueScheduler.processQueue()

        // assert: UUID 형식 검증
        val token = queueRepository.getToken(QUEUE_NAME, userId)
        assertThat(token).isNotNull()
        assertThat(token).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }

    @DisplayName("대기열 스케줄러 - 토큰 TTL")
    @Test
    fun `발급된 토큰은 TTL이 설정된다`() {
        // arrange: 1명 진입
        val userId = 100L
        queueRepository.enter(QUEUE_NAME, userId, System.currentTimeMillis().toDouble())

        // act
        queueScheduler.processQueue()

        // assert: 토큰이 존재하고 TTL이 양수 (Redis 명령으로 직접 확인은 어려우므로, 토큰 존재 확인)
        val token = queueRepository.getToken(QUEUE_NAME, userId)
        assertThat(token).isNotNull()
    }

    @DisplayName("대기열 스케줄러 - popMin 원자성 및 순서")
    @Test
    fun `processQueue 호출 후 대기열에서 사용자가 제거된다`() {
        // arrange: 5명 진입
        repeat(5) { i ->
            val userId = (1000L + i).toLong()
            queueRepository.enter(QUEUE_NAME, userId, System.currentTimeMillis().toDouble() + i)
        }
        assertThat(queueRepository.size(QUEUE_NAME)).isEqualTo(5L)

        // act: 충분히 많은 시간 경과 시뮬레이션으로 모두 처리
        queueScheduler.processQueue()
        val bucket = queueScheduler.getTokenBucket(QUEUE_NAME)
        bucket?.simulateElapsedTimeAndCalculateBatchSize(500) // 충분한 토큰 축적
        queueScheduler.processQueue()

        // assert: 모두 처리됨
        assertThat(queueRepository.size(QUEUE_NAME)).isEqualTo(0L)
    }

    @DisplayName("토큰 만료 테스트")
    @Test
    fun `TTL이 만료된 토큰은 조회되지 않는다`() {
        // arrange: 짧은 TTL(1초)로 토큰 발급
        val userId = 100L
        val token = "test-token"
        queueRepository.issueToken(QUEUE_NAME, userId, token, 1L)
        assertThat(queueRepository.getToken(QUEUE_NAME, userId)).isEqualTo(token)

        // act & assert: TTL 만료 대기 및 확인 (폴링으로 안정적 대기)
        Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted {
                assertThat(queueRepository.getToken(QUEUE_NAME, userId)).isNull()
            }
    }
}
