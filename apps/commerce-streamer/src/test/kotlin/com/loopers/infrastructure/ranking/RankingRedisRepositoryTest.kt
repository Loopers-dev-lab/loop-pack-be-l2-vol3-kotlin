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
@DisplayName("RankingRedisRepository")
class RankingRedisRepositoryTest @Autowired constructor(
    private val rankingRepository: RankingRepository,
    private val redisCleanUp: RedisCleanUp,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {

    private val today = LocalDate.of(2026, 4, 8)
    private val key = RedisKeys.rankingKey(today.format(DateTimeFormatter.ofPattern("yyyyMMdd")))

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @DisplayName("ZINCRBY 점수 증가 시,")
    @Nested
    inner class IncrementScore {

        @DisplayName("점수가 정상 증가한다.")
        @Test
        fun incrementsScore() {
            // act
            rankingRepository.incrementScore(today, 100L, 0.1)
            rankingRepository.incrementScore(today, 100L, 0.2)

            // assert
            val score = redisTemplate.opsForZSet().score(key, "100")
            assertThat(score).isCloseTo(0.3, org.assertj.core.data.Offset.offset(0.0001))
        }
    }

    @DisplayName("키가 없을 때 ZINCRBY 시,")
    @Nested
    inner class NewKey {

        @DisplayName("키가 자동 생성되고 TTL 2일이 설정된다.")
        @Test
        fun createsKeyWithTtl() {
            // act
            rankingRepository.incrementScore(today, 100L, 0.1)

            // assert
            val ttl = redisTemplate.getExpire(key)
            assertThat(ttl).isGreaterThan(0)
            assertThat(ttl).isLessThanOrEqualTo(2 * 24 * 60 * 60L)
        }
    }

    @DisplayName("이미 TTL이 있는 키에 ZINCRBY 시,")
    @Nested
    inner class ExistingKeyWithTtl {

        @DisplayName("TTL이 변경되지 않는다.")
        @Test
        fun doesNotChangeTtl() {
            // arrange
            rankingRepository.incrementScore(today, 100L, 0.1)
            val ttlBefore = redisTemplate.getExpire(key)

            // act
            rankingRepository.incrementScore(today, 200L, 0.5)

            // assert
            val ttlAfter = redisTemplate.getExpire(key)
            assertThat(ttlAfter).isLessThanOrEqualTo(ttlBefore!!)
        }
    }
}
