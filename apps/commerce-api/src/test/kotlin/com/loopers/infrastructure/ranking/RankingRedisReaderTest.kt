package com.loopers.infrastructure.ranking

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.DefaultTypedTuple
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import kotlin.math.abs
import java.time.Duration
import java.time.ZonedDateTime

class RankingRedisReaderTest {
    private val redisTemplate = mockk<RedisTemplate<String, String>>(relaxed = true)
    private val zSetOperations = mockk<ZSetOperations<String, String>>(relaxed = true)
    private val rankingRedisReader = RankingRedisReader(redisTemplate)

    init {
        every { redisTemplate.opsForZSet() } returns zSetOperations
    }

    @Test
    fun `점수_증가는_occurredAt_기준_일간_키에_반영된다`() {
        val occurredAt = ZonedDateTime.parse("2026-04-10T23:59:00+09:00[Asia/Seoul]")

        rankingRedisReader.incrementScore(productId = 1L, occurredAt = occurredAt, score = 3.0)

        verify(exactly = 1) {
            zSetOperations.incrementScore("ranking:all:20260410", "1", 3.0)
        }
        verify(exactly = 1) {
            redisTemplate.expire("ranking:all:20260410", Duration.ofDays(2))
        }
    }

    @Test
    fun `랭킹_페이지는_레디스_내림차순_순서를_그대로_반환한다`() {
        every {
            zSetOperations.reverseRangeWithScores("ranking:all:20260411", 0, 1)
        } returns linkedSetOf(
            DefaultTypedTuple("20", 1.2),
            DefaultTypedTuple("10", 0.8),
        )

        val result = rankingRedisReader.getPage(date = "20260411", start = 0, end = 1)

        assertThat(result.map { Triple(it.rank, it.productId, it.score) })
            .containsExactly(
                Triple(1L, 20L, 1.2),
                Triple(2L, 10L, 0.8),
            )
    }

    @Test
    fun `carry_over는_상위_N개를_배수와_함께_다음날_키로_복사한다`() {
        every {
            zSetOperations.reverseRangeWithScores("ranking:all:20260410", 0, 1)
        } returns linkedSetOf(
            DefaultTypedTuple("10", 10.0),
            DefaultTypedTuple("20", 6.0),
        )

        rankingRedisReader.carryOver(
            fromDate = "20260410",
            toDate = "20260411",
            limit = 2,
            multiplier = 0.2,
        )

        verify(exactly = 1) {
            zSetOperations.add(
                "ranking:all:20260411",
                match { tuples ->
                    val actualScores = tuples.associate { requireNotNull(it.value) to requireNotNull(it.score) }
                    actualScores.keys == setOf("10", "20") &&
                        abs((actualScores["10"] ?: 0.0) - 2.0) < 1e-9 &&
                        abs((actualScores["20"] ?: 0.0) - 1.2) < 1e-9
                },
            )
        }
        verify(exactly = 1) {
            redisTemplate.expire("ranking:all:20260411", Duration.ofDays(2))
        }
    }
}
