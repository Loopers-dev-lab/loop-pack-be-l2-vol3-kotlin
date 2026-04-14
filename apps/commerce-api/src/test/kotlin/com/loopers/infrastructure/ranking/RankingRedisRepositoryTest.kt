package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.config.redis.RedisKeys
import com.loopers.domain.ranking.RankingRepository
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SpringBootTest
@DisplayName("RankingRedisRepository (commerce-api)")
class RankingRedisRepositoryTest @Autowired constructor(
    private val rankingRepository: RankingRepository,
    private val redisCleanUp: RedisCleanUp,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {

    private val today = LocalDate.of(2026, 4, 9)
    private val key = RedisKeys.rankingKey(today.format(DateTimeFormatter.ofPattern("yyyyMMdd")))

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @DisplayName("ZREVRANGE WITHSCORES 조회 시,")
    @Nested
    inner class GetTopRankings {

        @DisplayName("상위 N개를 점수 내림차순으로 조회한다.")
        @Test
        fun returnsTopNByScoreDesc() {
            // arrange
            redisTemplate.opsForZSet().add(key, "100", 50.0)
            redisTemplate.opsForZSet().add(key, "200", 30.0)
            redisTemplate.opsForZSet().add(key, "300", 80.0)

            // act
            val result = rankingRepository.getTopRankings(today, offset = 0, count = 3)

            // assert
            assertThat(result).hasSize(3)
            assertThat(result[0].productId).isEqualTo(300L)
            assertThat(result[0].score).isEqualTo(80.0)
            assertThat(result[1].productId).isEqualTo(100L)
            assertThat(result[2].productId).isEqualTo(200L)
        }

        @DisplayName("페이지네이션이 정상 동작한다 (offset=2, count=2).")
        @Test
        fun paginatesCorrectly() {
            // arrange — 5개 상품 등록
            redisTemplate.opsForZSet().add(key, "1", 50.0)
            redisTemplate.opsForZSet().add(key, "2", 40.0)
            redisTemplate.opsForZSet().add(key, "3", 30.0)
            redisTemplate.opsForZSet().add(key, "4", 20.0)
            redisTemplate.opsForZSet().add(key, "5", 10.0)

            // act — 3번째부터 2개 (0-based offset=2)
            val result = rankingRepository.getTopRankings(today, offset = 2, count = 2)

            // assert — score 기준 내림차순 3번째, 4번째
            assertThat(result).hasSize(2)
            assertThat(result[0].productId).isEqualTo(3L)
            assertThat(result[1].productId).isEqualTo(4L)
        }
    }

    @DisplayName("ZREVRANK 조회 시,")
    @Nested
    inner class GetRank {

        @DisplayName("존재하는 상품의 순위를 0-based로 반환한다.")
        @Test
        fun returnsZeroBasedRank() {
            // arrange
            redisTemplate.opsForZSet().add(key, "100", 50.0)
            redisTemplate.opsForZSet().add(key, "200", 30.0)
            redisTemplate.opsForZSet().add(key, "300", 80.0)

            // act
            val rank = rankingRepository.getRank(today, 300L)

            // assert — 300이 1위(0-based: 0)
            assertThat(rank).isEqualTo(0L)
        }

        @DisplayName("존재하지 않는 상품은 null을 반환한다.")
        @Test
        fun returnsNullForNonExisting() {
            // arrange
            redisTemplate.opsForZSet().add(key, "100", 50.0)

            // act
            val rank = rankingRepository.getRank(today, 999L)

            // assert
            assertThat(rank).isNull()
        }
    }

    @DisplayName("carryOver 시,")
    @Nested
    inner class CarryOver {

        private val tomorrow = today.plusDays(1)
        private val tomorrowKey = RedisKeys.rankingKey(tomorrow.format(DateTimeFormatter.ofPattern("yyyyMMdd")))

        @DisplayName("전날 점수에 가중치를 곱한 값이 다음날 키에 복사된다.")
        @Test
        fun copiesScoresWithWeight() {
            // arrange
            redisTemplate.opsForZSet().add(key, "100", 100.0)
            redisTemplate.opsForZSet().add(key, "200", 50.0)

            // act
            rankingRepository.carryOver(today, tomorrow, 0.1)

            // assert
            val score100 = redisTemplate.opsForZSet().score(tomorrowKey, "100")
            val score200 = redisTemplate.opsForZSet().score(tomorrowKey, "200")
            assertThat(score100).isCloseTo(10.0, org.assertj.core.data.Offset.offset(0.001))
            assertThat(score200).isCloseTo(5.0, org.assertj.core.data.Offset.offset(0.001))
        }

        @DisplayName("생성된 키에 TTL 2일이 설정된다.")
        @Test
        fun setsTtlOnNewKey() {
            // arrange
            redisTemplate.opsForZSet().add(key, "100", 100.0)

            // act
            rankingRepository.carryOver(today, tomorrow, 0.1)

            // assert
            val ttl = redisTemplate.getExpire(tomorrowKey)
            assertThat(ttl).isGreaterThan(0)
            assertThat(ttl).isLessThanOrEqualTo(2 * 24 * 60 * 60L)
        }
    }
}
