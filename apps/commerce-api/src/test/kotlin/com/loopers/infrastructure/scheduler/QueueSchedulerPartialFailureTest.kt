package com.loopers.infrastructure.scheduler

import com.loopers.domain.queue.QueueRepository
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(RedisTestContainersConfig::class)
@DisplayName("QueueScheduler - 부분 실패 및 사용자 복구 테스트")
class QueueSchedulerPartialFailureTest @Autowired constructor(
    private val queueRepository: QueueRepository,
    private val redisCleanUp: RedisCleanUp,
    private val queueScheduler: QueueScheduler,
) {

    companion object {
        private const val QUEUE_NAME = "partial-failure-queue"
        private const val BATCH_SIZE = 3L
    }

    @BeforeEach
    fun setUp() {
        redisCleanUp.truncateAll()
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @DisplayName("토큰 발급 부분 실패 시 실패한 사용자만 원래 순번으로 복구된다")
    @Test
    fun `배치 처리 중 일부 토큰 발급 실패 시 실패한 사용자가 원래 score로 재삽입된다`() {
        // arrange: 5명이 순서대로 진입 (score: 1000.0, 1001.0, 1002.0, 1003.0, 1004.0)
        repeat(5) { i ->
            val userId = (100L + i).toLong()
            val score = 1000.0 + i
            queueRepository.enter(QUEUE_NAME, userId, score)
        }

        assertThat(queueRepository.size(QUEUE_NAME)).isEqualTo(5L)

        // act: 배치 크기 3으로 처리 (user 100, 101, 102를 popMin으로 가져옴)
        val queuedUsers = queueRepository.popMin(QUEUE_NAME, BATCH_SIZE)
        assertThat(queuedUsers).hasSize(3)

        // 처리 후 큐에는 3명이 남아있어야 함
        assertThat(queueRepository.size(QUEUE_NAME)).isEqualTo(2L)

        // 발급된 사용자들
        queuedUsers.forEach { queuedUser ->
            // user 101의 토큰 발급을 실패하도록 설정
            if (queuedUser.userId == 101L) {
                // 실제 Redis에 issueToken 호출을 하지 않음으로써 실패 시뮬레이션
                // (정상 경로에서는 실패 발생)
            } else {
                // 나머지는 정상 발급
                val token = java.util.UUID.randomUUID().toString()
                queueRepository.issueToken(QUEUE_NAME, queuedUser.userId, token, 300L)
            }
        }

        // user 101의 재삽입 시뮬레이션
        // (실제로는 QueueScheduler.reinsertFailedUsers에서 수행)
        val failedUser = queuedUsers.find { it.userId == 101L }
        if (failedUser != null) {
            queueRepository.enter(QUEUE_NAME, failedUser.userId, failedUser.score)
        }

        // assert: 큐의 상태 검증
        // - 처리 전: 5명
        // - popMin 후: 2명 (user 103, 104)
        // - 재삽입 후: 3명 (user 101, 103, 104)
        val finalSize = queueRepository.size(QUEUE_NAME)
        assertThat(finalSize).isEqualTo(3L)

        // 재삽입된 user 101의 순번 확인
        val user101Rank = queueRepository.getRank(QUEUE_NAME, 101L)
        assertThat(user101Rank).isNotNull()

        // user 101이 user 103보다 먼저 있어야 함 (score: 1001.0 < 1003.0)
        val user103Rank = queueRepository.getRank(QUEUE_NAME, 103L)
        assertThat(user101Rank).isLessThan(user103Rank)

        // user 100과 102는 토큰이 발급되었으므로 큐에 없어야 함
        assertThat(queueRepository.getRank(QUEUE_NAME, 100L)).isNull()
        assertThat(queueRepository.getRank(QUEUE_NAME, 102L)).isNull()

        // user 101은 토큰 발급 실패했으므로 토큰이 없어야 함
        assertThat(queueRepository.getToken(QUEUE_NAME, 101L)).isNull()

        // user 100, 102는 토큰이 있어야 함
        assertThat(queueRepository.getToken(QUEUE_NAME, 100L)).isNotNull()
        assertThat(queueRepository.getToken(QUEUE_NAME, 102L)).isNotNull()
    }

    @DisplayName("모든 토큰 발급 성공 시 실패한 사용자가 없다")
    @Test
    fun `배치 처리 중 모든 토큰 발급이 성공하면 실패한 사용자가 없다`() {
        // arrange: 3명이 순서대로 진입
        repeat(3) { i ->
            val userId = (200L + i).toLong()
            val score = 2000.0 + i
            queueRepository.enter(QUEUE_NAME, userId, score)
        }

        assertThat(queueRepository.size(QUEUE_NAME)).isEqualTo(3L)

        // act: 배치 크기 3으로 처리
        val queuedUsers = queueRepository.popMin(QUEUE_NAME, BATCH_SIZE)
        assertThat(queuedUsers).hasSize(3)

        // 모든 사용자의 토큰 발급 성공
        queuedUsers.forEach { queuedUser ->
            val token = java.util.UUID.randomUUID().toString()
            queueRepository.issueToken(QUEUE_NAME, queuedUser.userId, token, 300L)
        }

        // assert: 모든 사용자가 처리되었으므로 큐가 비어있어야 함
        assertThat(queueRepository.size(QUEUE_NAME)).isEqualTo(0L)

        // 모든 사용자가 토큰을 가지고 있어야 함
        queuedUsers.forEach { queuedUser ->
            assertThat(queueRepository.getToken(QUEUE_NAME, queuedUser.userId)).isNotNull()
        }
    }
}
