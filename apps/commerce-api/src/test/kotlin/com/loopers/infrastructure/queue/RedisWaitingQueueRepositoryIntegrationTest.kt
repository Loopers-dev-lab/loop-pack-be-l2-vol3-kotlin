package com.loopers.infrastructure.queue

import com.loopers.domain.queue.WaitingQueueRepository
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@DisplayName("RedisWaitingQueueRepository 통합 테스트")
@SpringBootTest
class RedisWaitingQueueRepositoryIntegrationTest
@Autowired
constructor(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val redisCleanUp: RedisCleanUp,
) {
    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("대기열에 진입하면 순번이 부여된다")
    inner class Enter {
        @Test
        @DisplayName("첫 진입 시 position == 0, totalWaiting == 1")
        fun enter_success() {
            val result = waitingQueueRepository.enter(1L)

            assertAll(
                { assertThat(result.position).isEqualTo(0) },
                { assertThat(result.totalWaiting).isEqualTo(1) },
            )
        }

        @Test
        @DisplayName("3명 순차 진입 시 position 0, 1, 2")
        fun enter_multipleUsers() {
            val r1 = waitingQueueRepository.enter(1L)
            val r2 = waitingQueueRepository.enter(2L)
            val r3 = waitingQueueRepository.enter(3L)

            assertAll(
                { assertThat(r1.position).isEqualTo(0) },
                { assertThat(r2.position).isEqualTo(1) },
                { assertThat(r3.position).isEqualTo(2) },
            )
        }

        @Test
        @DisplayName("동일 userId 재진입 시 기존 순번 유지 (ZADD NX)")
        fun enter_duplicateUser() {
            waitingQueueRepository.enter(1L)
            waitingQueueRepository.enter(2L)

            val duplicate = waitingQueueRepository.enter(1L)

            assertAll(
                { assertThat(duplicate.position).isEqualTo(0) },
                { assertThat(duplicate.totalWaiting).isEqualTo(2) },
            )
        }
    }

    @Nested
    @DisplayName("순번을 조회하면 현재 위치를 반환한다")
    inner class GetPosition {
        @Test
        @DisplayName("진입한 유저의 순번을 반환한다")
        fun getPosition_exists() {
            waitingQueueRepository.enter(1L)
            waitingQueueRepository.enter(2L)
            waitingQueueRepository.enter(3L)

            val result = waitingQueueRepository.getPosition(2L)

            assertThat(result).isNotNull
            assertAll(
                { assertThat(result!!.position).isEqualTo(1) },
                { assertThat(result!!.totalWaiting).isEqualTo(3) },
            )
        }

        @Test
        @DisplayName("미진입 유저는 null을 반환한다")
        fun getPosition_notExists() {
            val result = waitingQueueRepository.getPosition(999L)

            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("동시 진입 시 순서가 유일하게 보장된다")
    inner class ConcurrentEnter {
        @Test
        @DisplayName("20명 동시 진입 시 position이 모두 유일하다")
        fun enter_concurrent_uniquePositions() {
            val latch = CountDownLatch(20)
            val executor = Executors.newFixedThreadPool(10)
            val positions = ConcurrentHashMap<Long, Long>()
            (1L..20L).forEach { userId ->
                executor.submit {
                    try {
                        val result = waitingQueueRepository.enter(userId)
                        positions[userId] = result.position
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()
            assertThat(positions.values.toSet()).hasSize(20)
        }
    }

    @Nested
    @DisplayName("전체 대기 인원을 조회한다")
    inner class Size {
        @Test
        @DisplayName("빈 대기열은 0을 반환한다")
        fun size_empty() {
            assertThat(waitingQueueRepository.size()).isEqualTo(0)
        }

        @Test
        @DisplayName("3명 진입 후 3을 반환한다")
        fun size_afterEnter() {
            waitingQueueRepository.enter(1L)
            waitingQueueRepository.enter(2L)
            waitingQueueRepository.enter(3L)

            assertThat(waitingQueueRepository.size()).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("대기열에서 선두 N명을 꺼낸다")
    inner class PopFront {
        @Test
        @DisplayName("5명 중 2명 pop → 선두 2명 반환, 3명 잔여")
        fun popFront_partial() {
            (1L..5L).forEach { waitingQueueRepository.enter(it) }

            val popped = waitingQueueRepository.popFront(2)

            assertAll(
                { assertThat(popped).hasSize(2) },
                { assertThat(popped).containsExactly(1L, 2L) },
                { assertThat(waitingQueueRepository.size()).isEqualTo(3) },
            )
        }

        @Test
        @DisplayName("빈 대기열에서 pop → 빈 리스트")
        fun popFront_empty() {
            val popped = waitingQueueRepository.popFront(5)

            assertThat(popped).isEmpty()
        }
    }
}
