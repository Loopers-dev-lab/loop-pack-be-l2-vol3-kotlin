package com.loopers.infrastructure.ranking

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

@SpringBootTest
@Import(MySqlTestContainersConfig::class, RedisTestContainersConfig::class)
class RedisRankingRepositoryIntegrationTest {

    @Autowired
    private lateinit var redisRankingRepository: RedisRankingRepository

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Autowired
    private lateinit var redisCleanUp: RedisCleanUp

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    inner class GetTopRankings {

        @Test
        fun `ZSET에 점수가 있으면 score 내림차순으로 조회된다`() {
            redisTemplate.opsForZSet().add(KEY, "1", 10.0)
            redisTemplate.opsForZSet().add(KEY, "2", 30.0)
            redisTemplate.opsForZSet().add(KEY, "3", 20.0)

            val result = redisRankingRepository.getTopRankings(DATE, 0, 10)

            assertThat(result).hasSize(3)
            assertThat(result[0].productId).isEqualTo(2L)
            assertThat(result[0].score).isEqualTo(30.0)
            assertThat(result[0].rank).isEqualTo(0)
            assertThat(result[1].productId).isEqualTo(3L)
            assertThat(result[2].productId).isEqualTo(1L)
        }

        @Test
        fun `offset과 size로 페이지네이션이 동작한다`() {
            for (i in 1..5) {
                redisTemplate.opsForZSet().add(KEY, i.toString(), (60 - i * 10).toDouble())
            }

            val page2 = redisRankingRepository.getTopRankings(DATE, 2, 2)

            assertThat(page2).hasSize(2)
            assertThat(page2[0].rank).isEqualTo(2)
            assertThat(page2[1].rank).isEqualTo(3)
        }

        @Test
        fun `빈 ZSET이면 빈 리스트를 반환한다`() {
            val result = redisRankingRepository.getTopRankings(DATE, 0, 10)

            assertThat(result).isEmpty()
        }
    }

    @Nested
    inner class GetRank {

        @Test
        fun `존재하는 멤버의 순위를 반환한다`() {
            redisTemplate.opsForZSet().add(KEY, "1", 100.0)
            redisTemplate.opsForZSet().add(KEY, "2", 50.0)

            val rank = redisRankingRepository.getRank(DATE, 2L)

            assertThat(rank).isEqualTo(1L) // 0-based, 2번째
        }

        @Test
        fun `존재하지 않는 멤버는 null을 반환한다`() {
            val rank = redisRankingRepository.getRank(DATE, 999L)

            assertThat(rank).isNull()
        }
    }

    @Nested
    inner class GetScore {

        @Test
        fun `존재하는 멤버의 점수를 반환한다`() {
            redisTemplate.opsForZSet().add(KEY, "1", 42.5)

            val score = redisRankingRepository.getScore(DATE, 1L)

            assertThat(score).isEqualTo(42.5)
        }
    }

    @Nested
    inner class GetTotalCount {

        @Test
        fun `ZSET의 전체 멤버 수를 반환한다`() {
            redisTemplate.opsForZSet().add(KEY, "1", 10.0)
            redisTemplate.opsForZSet().add(KEY, "2", 20.0)

            val count = redisRankingRepository.getTotalCount(DATE)

            assertThat(count).isEqualTo(2L)
        }
    }

    companion object {
        private const val DATE = "20260409"
        private const val KEY = "ranking:all:$DATE"
    }
}
