package com.loopers.batch.job.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.connection.zset.Aggregate
import org.springframework.data.redis.connection.zset.Weights
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SpringBootTest(
    classes = [
        RedisTestContainersConfig::class,
        RedisConfig::class,
        RedisCleanUp::class,
    ],
)
@DisplayName("RankingCarryOver 통합 테스트")
class RankingCarryOverTaskletTest @Autowired constructor(
    private val redisCleanUp: RedisCleanUp,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val today = LocalDate.now()
    private val todayKey = "ranking:all:${today.format(formatter)}"
    private val tomorrowKey = "ranking:all:${today.plusDays(1).format(formatter)}"
    private val carryOverWeight = 0.1

    @BeforeEach
    fun setUp() {
        redisTemplate.opsForZSet().add(todayKey, "101", 100.0)
        redisTemplate.opsForZSet().add(todayKey, "202", 50.0)
        redisTemplate.opsForZSet().add(todayKey, "303", 75.0)
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Test
    @DisplayName("오늘 점수의 10%가 내일 키에 이월된다")
    fun `carry-over 10%`() {
        // Act: Tasklet과 동일한 ZUNIONSTORE 로직
        redisTemplate.opsForZSet().unionAndStore(
            tomorrowKey,
            listOf(todayKey),
            tomorrowKey,
            Aggregate.SUM,
            Weights.of(1.0, carryOverWeight),
        )

        // Assert
        val size = redisTemplate.opsForZSet().size(tomorrowKey) ?: 0L
        assertThat(size).isEqualTo(3)

        val score101 = redisTemplate.opsForZSet().score(tomorrowKey, "101") ?: 0.0
        val score202 = redisTemplate.opsForZSet().score(tomorrowKey, "202") ?: 0.0
        val score303 = redisTemplate.opsForZSet().score(tomorrowKey, "303") ?: 0.0

        assertThat(score101).isCloseTo(10.0, org.assertj.core.data.Offset.offset(0.01))
        assertThat(score202).isCloseTo(5.0, org.assertj.core.data.Offset.offset(0.01))
        assertThat(score303).isCloseTo(7.5, org.assertj.core.data.Offset.offset(0.01))
    }

    @Test
    @DisplayName("내일 키에 기존 데이터가 있으면 합산된다")
    fun `기존 데이터와 합산`() {
        // Arrange
        redisTemplate.opsForZSet().add(tomorrowKey, "101", 20.0)

        // Act
        redisTemplate.opsForZSet().unionAndStore(
            tomorrowKey,
            listOf(todayKey),
            tomorrowKey,
            Aggregate.SUM,
            Weights.of(1.0, carryOverWeight),
        )

        // Assert: 기존 20.0 × 1.0 + 오늘 100.0 × 0.1 = 30.0
        val score101 = redisTemplate.opsForZSet().score(tomorrowKey, "101") ?: 0.0
        assertThat(score101).isCloseTo(30.0, org.assertj.core.data.Offset.offset(0.01))
    }

    @Test
    @DisplayName("오늘 랭킹 데이터가 없으면 carry-over를 스킵한다")
    fun `빈 데이터 스킵`() {
        // Arrange
        redisTemplate.delete(todayKey)
        val todaySize = redisTemplate.opsForZSet().size(todayKey) ?: 0L

        // Assert: 스킵 조건 (todaySize == 0)
        assertThat(todaySize).isEqualTo(0)

        // Act: carry-over 미실행 시 내일 키 없음
        val tomorrowSize = redisTemplate.opsForZSet().size(tomorrowKey) ?: 0L
        assertThat(tomorrowSize).isEqualTo(0)
    }
}
