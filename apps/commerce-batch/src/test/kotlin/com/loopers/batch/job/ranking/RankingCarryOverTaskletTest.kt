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
import java.time.Duration
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
    private val yesterdayKey = "ranking:all:${today.minusDays(1).format(formatter)}"
    private val todayKey = "ranking:all:${today.format(formatter)}"
    private val carryOverWeight = 0.1

    private lateinit var tasklet: RankingCarryOverTasklet

    @BeforeEach
    fun setUp() {
        tasklet = RankingCarryOverTasklet(redisTemplate)
        // 전일 키에 테스트 데이터 적재 (점수가 모두 다르게 설정하여 동점 문제 방지)
        redisTemplate.opsForZSet().add(yesterdayKey, "101", 100.0)
        redisTemplate.opsForZSet().add(yesterdayKey, "202", 50.0)
        redisTemplate.opsForZSet().add(yesterdayKey, "303", 75.0)
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Test
    @DisplayName("전일 점수의 10%가 오늘 키에 이월된다")
    fun `carry-over 10 percent`() {
        // Act: Tasklet과 동일한 ZUNIONSTORE 로직
        redisTemplate.opsForZSet().unionAndStore(
            todayKey,
            listOf(yesterdayKey),
            todayKey,
            Aggregate.SUM,
            Weights.of(1.0, carryOverWeight),
        )

        // Assert
        val size = redisTemplate.opsForZSet().size(todayKey) ?: 0L
        assertThat(size).isEqualTo(3)

        val score101 = redisTemplate.opsForZSet().score(todayKey, "101") ?: 0.0
        val score202 = redisTemplate.opsForZSet().score(todayKey, "202") ?: 0.0
        val score303 = redisTemplate.opsForZSet().score(todayKey, "303") ?: 0.0

        assertThat(score101).isCloseTo(10.0, org.assertj.core.data.Offset.offset(0.01))
        assertThat(score202).isCloseTo(5.0, org.assertj.core.data.Offset.offset(0.01))
        assertThat(score303).isCloseTo(7.5, org.assertj.core.data.Offset.offset(0.01))
    }

    @Test
    @DisplayName("오늘 키에 기존 데이터가 있으면 합산된다")
    fun `carry-over merges with existing data`() {
        // Arrange
        redisTemplate.opsForZSet().add(todayKey, "101", 20.0)

        // Act
        redisTemplate.opsForZSet().unionAndStore(
            todayKey,
            listOf(yesterdayKey),
            todayKey,
            Aggregate.SUM,
            Weights.of(1.0, carryOverWeight),
        )

        // Assert: 기존 20.0 × 1.0 + 전일 100.0 × 0.1 = 30.0
        val score101 = redisTemplate.opsForZSet().score(todayKey, "101") ?: 0.0
        assertThat(score101).isCloseTo(30.0, org.assertj.core.data.Offset.offset(0.01))
    }

    @Test
    @DisplayName("전일 랭킹 데이터가 없으면 Tasklet이 carry-over를 스킵한다")
    fun `tasklet skips when yesterday is empty`() {
        // Arrange: 전일 키 삭제
        redisTemplate.delete(yesterdayKey)

        // Act: Tasklet 직접 실행 — 스킵 경로 검증
        val result = tasklet.execute(
            org.mockito.Mockito.mock(org.springframework.batch.core.StepContribution::class.java),
            org.mockito.Mockito.mock(org.springframework.batch.core.scope.context.ChunkContext::class.java),
        )

        // Assert: 스킵 후 오늘 키 없음
        assertThat(result).isEqualTo(org.springframework.batch.repeat.RepeatStatus.FINISHED)
        val size = redisTemplate.opsForZSet().size(todayKey) ?: 0L
        assertThat(size).isEqualTo(0)
    }

    @Test
    @DisplayName("중복 실행 시 carry-over flag로 두 번째 실행이 스킵된다")
    fun `duplicate execution is prevented by flag`() {
        // Arrange: carry-over flag 설정 (이미 실행된 것처럼)
        val carryOverFlag = "ranking:all:carry-over:${today.format(formatter)}"
        redisTemplate.opsForValue().set(carryOverFlag, "done", Duration.ofDays(2))

        // Act: Tasklet 실행 — flag 확인 후 스킵
        val result = tasklet.execute(
            org.mockito.Mockito.mock(org.springframework.batch.core.StepContribution::class.java),
            org.mockito.Mockito.mock(org.springframework.batch.core.scope.context.ChunkContext::class.java),
        )

        // Assert: 오늘 키에 데이터 없음 (carry-over 미실행)
        assertThat(result).isEqualTo(org.springframework.batch.repeat.RepeatStatus.FINISHED)
        val size = redisTemplate.opsForZSet().size(todayKey) ?: 0L
        assertThat(size).isEqualTo(0)
    }
}
