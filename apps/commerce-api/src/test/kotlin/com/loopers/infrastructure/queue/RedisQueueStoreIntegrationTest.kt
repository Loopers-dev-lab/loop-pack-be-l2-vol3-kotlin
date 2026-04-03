package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [RedisTestContainersConfig::class, RedisConfig::class, RedisCleanUp::class, RedisQueueStoreImpl::class])
@DisplayName("RedisQueueStoreImpl 통합 테스트")
class RedisQueueStoreIntegrationTest @Autowired constructor(
    private val queueStore: RedisQueueStoreImpl,
    private val redisCleanUp: RedisCleanUp,
) {
    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("add")
    inner class Add {

        @Test
        @DisplayName("새로운 멤버를 대기열에 추가하면 true 반환")
        fun `새로운 멤버 추가`() {
            val result = queueStore.add(1L, 1000.0)

            assertThat(result).isTrue()
            assertThat(queueStore.size()).isEqualTo(1)
        }

        @Test
        @DisplayName("동일 멤버 중복 추가 시 false 반환")
        fun `중복 멤버 추가 방지`() {
            queueStore.add(1L, 1000.0)

            val result = queueStore.add(1L, 2000.0)

            assertThat(result).isFalse()
            assertThat(queueStore.size()).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("rank")
    inner class Rank {

        @Test
        @DisplayName("대기열에 있는 멤버의 순번을 반환한다 (0-based)")
        fun `순번 조회`() {
            queueStore.add(1L, 1000.0)
            queueStore.add(2L, 2000.0)
            queueStore.add(3L, 3000.0)

            assertThat(queueStore.rank(1L)).isEqualTo(0)
            assertThat(queueStore.rank(2L)).isEqualTo(1)
            assertThat(queueStore.rank(3L)).isEqualTo(2)
        }

        @Test
        @DisplayName("대기열에 없는 멤버는 null 반환")
        fun `없는 멤버 조회`() {
            assertThat(queueStore.rank(999L)).isNull()
        }
    }

    @Nested
    @DisplayName("popMin")
    inner class PopMin {

        @Test
        @DisplayName("score 낮은 순으로 N명을 꺼낸다")
        fun `N명 팝`() {
            queueStore.add(3L, 3000.0)
            queueStore.add(1L, 1000.0)
            queueStore.add(2L, 2000.0)

            val popped = queueStore.popMin(2)

            assertThat(popped).containsExactly(1L, 2L)
            assertThat(queueStore.size()).isEqualTo(1)
        }

        @Test
        @DisplayName("대기열보다 많은 수를 요청하면 전부 반환")
        fun `전부 팝`() {
            queueStore.add(1L, 1000.0)
            queueStore.add(2L, 2000.0)

            val popped = queueStore.popMin(10)

            assertThat(popped).containsExactly(1L, 2L)
            assertThat(queueStore.size()).isEqualTo(0)
        }

        @Test
        @DisplayName("빈 대기열에서 popMin은 빈 리스트 반환")
        fun `빈 대기열`() {
            val popped = queueStore.popMin(5)

            assertThat(popped).isEmpty()
        }
    }

    @Nested
    @DisplayName("remove")
    inner class Remove {

        @Test
        @DisplayName("대기열에서 멤버를 제거하면 true 반환")
        fun `멤버 제거`() {
            queueStore.add(1L, 1000.0)

            val result = queueStore.remove(1L)

            assertThat(result).isTrue()
            assertThat(queueStore.size()).isEqualTo(0)
        }

        @Test
        @DisplayName("없는 멤버를 제거하면 false 반환")
        fun `없는 멤버 제거`() {
            val result = queueStore.remove(999L)

            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("동시 진입 순서 보장")
    inner class Ordering {

        @Test
        @DisplayName("score(timestamp) 순으로 순서가 보장된다")
        fun `순서 보장`() {
            for (i in 100L downTo 1L) {
                queueStore.add(i, i.toDouble())
            }

            val popped = queueStore.popMin(100)

            assertThat(popped).isEqualTo((1L..100L).toList())
        }
    }
}
