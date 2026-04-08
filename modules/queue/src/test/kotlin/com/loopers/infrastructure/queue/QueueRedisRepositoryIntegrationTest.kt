package com.loopers.infrastructure.queue

import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(RedisTestContainersConfig::class)
class QueueRedisRepositoryIntegrationTest @Autowired constructor(
    private val queueRedisRepository: QueueRedisRepository,
    private val redisCleanUp: RedisCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        redisCleanUp.truncateAll()
    }

    @DisplayName("addIfAbsent")
    @Nested
    inner class AddIfAbsent {
        @DisplayName("신규 사용자를 추가하면, true를 반환한다.")
        @Test
        fun returnsTrue_whenNewUser() {
            // act
            val result = queueRedisRepository.addIfAbsent(1L, 1000.0)

            // assert
            assertThat(result).isTrue()
        }

        @DisplayName("이미 존재하는 사용자를 추가하면, false를 반환한다.")
        @Test
        fun returnsFalse_whenDuplicateUser() {
            // arrange
            queueRedisRepository.addIfAbsent(1L, 1000.0)

            // act
            val result = queueRedisRepository.addIfAbsent(1L, 2000.0)

            // assert
            assertThat(result).isFalse()
        }
    }

    @DisplayName("getRank")
    @Nested
    inner class GetRank {
        @DisplayName("대기열에 있는 사용자의 순위를 반환한다.")
        @Test
        fun returnsRank_whenUserExists() {
            // arrange
            queueRedisRepository.addIfAbsent(1L, 1000.0)
            queueRedisRepository.addIfAbsent(2L, 2000.0)
            queueRedisRepository.addIfAbsent(3L, 3000.0)

            // act
            val rank = queueRedisRepository.getRank(2L)

            // assert
            assertThat(rank).isEqualTo(1L)
        }

        @DisplayName("대기열에 없는 사용자는 null을 반환한다.")
        @Test
        fun returnsNull_whenUserNotExists() {
            // act
            val rank = queueRedisRepository.getRank(999L)

            // assert
            assertThat(rank).isNull()
        }
    }

    @DisplayName("getSize")
    @Nested
    inner class GetSize {
        @DisplayName("대기열의 전체 인원 수를 반환한다.")
        @Test
        fun returnsTotalSize() {
            // arrange
            queueRedisRepository.addIfAbsent(1L, 1000.0)
            queueRedisRepository.addIfAbsent(2L, 2000.0)
            queueRedisRepository.addIfAbsent(3L, 3000.0)

            // act
            val size = queueRedisRepository.getSize()

            // assert
            assertThat(size).isEqualTo(3L)
        }

        @DisplayName("빈 대기열은 0을 반환한다.")
        @Test
        fun returnsZero_whenEmpty() {
            // act
            val size = queueRedisRepository.getSize()

            // assert
            assertThat(size).isEqualTo(0L)
        }
    }

    @DisplayName("popMin")
    @Nested
    inner class PopMin {
        @DisplayName("score가 가장 낮은 N명을 꺼낸다.")
        @Test
        fun popsLowestScoreUsers() {
            // arrange
            queueRedisRepository.addIfAbsent(1L, 1000.0)
            queueRedisRepository.addIfAbsent(2L, 2000.0)
            queueRedisRepository.addIfAbsent(3L, 3000.0)

            // act
            val popped = queueRedisRepository.popMin(2)

            // assert
            assertAll(
                { assertThat(popped).containsExactlyInAnyOrder("1", "2") },
                { assertThat(queueRedisRepository.getSize()).isEqualTo(1L) },
            )
        }

        @DisplayName("대기열이 비어있으면, 빈 Set을 반환한다.")
        @Test
        fun returnsEmptySet_whenEmpty() {
            // act
            val popped = queueRedisRepository.popMin(10)

            // assert
            assertThat(popped).isEmpty()
        }

        @DisplayName("요청한 수보다 대기열이 적으면, 있는 만큼만 꺼낸다.")
        @Test
        fun popsAvailable_whenLessThanRequested() {
            // arrange
            queueRedisRepository.addIfAbsent(1L, 1000.0)

            // act
            val popped = queueRedisRepository.popMin(10)

            // assert
            assertAll(
                { assertThat(popped).hasSize(1) },
                { assertThat(queueRedisRepository.getSize()).isEqualTo(0L) },
            )
        }
    }
}
