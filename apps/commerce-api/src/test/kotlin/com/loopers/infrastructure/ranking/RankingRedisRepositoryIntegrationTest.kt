package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.RankingRepository
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate

@SpringBootTest
@Import(RedisTestContainersConfig::class)
class RankingRedisRepositoryIntegrationTest @Autowired constructor(
    private val rankingRepository: RankingRepository,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
    private val redisCleanUp: RedisCleanUp,
) {
    private val testKey = "ranking:all:20260410"

    @AfterEach
    fun cleanUp() {
        redisCleanUp.truncateAll()
    }

    @BeforeEach
    fun setUp() {
        masterRedisTemplate.opsForZSet().add(testKey, "1", 100.0)
        masterRedisTemplate.opsForZSet().add(testKey, "2", 80.0)
        masterRedisTemplate.opsForZSet().add(testKey, "3", 60.0)
        masterRedisTemplate.opsForZSet().add(testKey, "4", 40.0)
        masterRedisTemplate.opsForZSet().add(testKey, "5", 20.0)
    }

    @DisplayName("상위 N개 랭킹 조회 시, ")
    @Nested
    inner class GetTopNWithScores {
        @DisplayName("점수 내림차순으로 반환한다.")
        @Test
        fun returnsDescendingOrder() {
            // act
            val result = rankingRepository.getTopNWithScores(testKey, 0, 2)

            // assert
            assertThat(result).hasSize(3)
            assertThat(result[0].productId).isEqualTo(1L)
            assertThat(result[0].score).isEqualTo(100.0)
            assertThat(result[1].productId).isEqualTo(2L)
            assertThat(result[2].productId).isEqualTo(3L)
        }

        @DisplayName("2페이지 offset으로 조회한다.")
        @Test
        fun returnsWithOffset() {
            // act
            val result = rankingRepository.getTopNWithScores(testKey, 3, 4)

            // assert
            assertThat(result).hasSize(2)
            assertThat(result[0].productId).isEqualTo(4L)
            assertThat(result[1].productId).isEqualTo(5L)
        }

        @DisplayName("존재하지 않는 키는 빈 리스트를 반환한다.")
        @Test
        fun returnsEmpty_whenKeyNotExists() {
            // act
            val result = rankingRepository.getTopNWithScores("ranking:all:99999999", 0, 9)

            // assert
            assertThat(result).isEmpty()
        }
    }

    @DisplayName("전체 개수 조회 시, ")
    @Nested
    inner class GetTotalCount {
        @DisplayName("ZSET 전체 멤버 수를 반환한다.")
        @Test
        fun returnsTotalCount() {
            // act
            val count = rankingRepository.getTotalCount(testKey)

            // assert
            assertThat(count).isEqualTo(5)
        }
    }

    @DisplayName("상품 순위 조회 시, ")
    @Nested
    inner class GetRank {
        @DisplayName("0-based 순위를 반환한다.")
        @Test
        fun returns0BasedRank() {
            // act
            val rank = rankingRepository.getRank(testKey, 1L)

            // assert
            assertThat(rank).isEqualTo(0)
        }

        @DisplayName("2위 상품은 1을 반환한다.")
        @Test
        fun returns1ForSecondPlace() {
            // act
            val rank = rankingRepository.getRank(testKey, 2L)

            // assert
            assertThat(rank).isEqualTo(1)
        }

        @DisplayName("순위에 없는 상품은 null을 반환한다.")
        @Test
        fun returnsNull_whenNotRanked() {
            // act
            val rank = rankingRepository.getRank(testKey, 999L)

            // assert
            assertThat(rank).isNull()
        }
    }
}
