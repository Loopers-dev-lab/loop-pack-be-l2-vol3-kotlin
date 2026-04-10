package com.loopers.domain.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.infrastructure.ranking.RankingEventJpaRepository
import com.loopers.infrastructure.ranking.RankingMetricJpaRepository
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SpringBootTest
@Import(RedisTestContainersConfig::class)
class RankingAggregationServiceIntegrationTest @Autowired constructor(
    private val rankingEventService: RankingEventService,
    private val rankingAggregationService: RankingAggregationService,
    private val rankingEventJpaRepository: RankingEventJpaRepository,
    private val rankingMetricJpaRepository: RankingMetricJpaRepository,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
    private val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    private val redisKey = "ranking:all:$today"

    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    @DisplayName("전체 파이프라인: 이벤트 저장 → 집계 → Redis 동기화")
    @Nested
    inner class FullPipeline {
        @DisplayName("여러 이벤트를 저장하고 집계하면, ranking_metric과 Redis ZSET에 반영된다.")
        @Test
        fun aggregatesAndSyncsToRedis() {
            // arrange - 이벤트 저장
            rankingEventService.saveViewBatch(
                listOf(
                    ViewCount(productId = 1L, count = 100),
                    ViewCount(productId = 2L, count = 50),
                ),
                "batch-1",
            )
            rankingEventService.saveLikeEvent(productId = 1L, eventId = 1L)
            rankingEventService.saveLikeEvent(productId = 2L, eventId = 2L)

            // act - 집계
            rankingAggregationService.aggregate()

            // assert - ranking_metric 확인
            val metrics = rankingMetricJpaRepository.findAllByRankingDate(today)
            assertThat(metrics).hasSize(2)

            val metric1 = metrics.find { it.productId == 1L }!!
            val metric2 = metrics.find { it.productId == 2L }!!
            assertAll(
                { assertThat(metric1.totalScore).isEqualTo(0.1 * 100 + 0.2) },  // view + like
                { assertThat(metric2.totalScore).isEqualTo(0.1 * 50 + 0.2) },
            )

            // assert - Redis ZSET 확인
            val redisScore1 = masterRedisTemplate.opsForZSet().score(redisKey, "1")
            val redisScore2 = masterRedisTemplate.opsForZSet().score(redisKey, "2")
            assertAll(
                { assertThat(redisScore1).isEqualTo(0.1 * 100 + 0.2) },
                { assertThat(redisScore2).isEqualTo(0.1 * 50 + 0.2) },
            )

            // assert - ranking_event aggregated 마킹 확인
            val unaggregated = rankingEventJpaRepository.findAll().filter { !it.aggregated }
            assertThat(unaggregated).isEmpty()
        }

        @DisplayName("두 번 집계해도 metric 점수가 누적되고, Redis가 올바르게 동기화된다.")
        @Test
        fun accumulatesAcrossMultipleAggregations() {
            // arrange - 1차 이벤트
            rankingEventService.saveViewBatch(
                listOf(ViewCount(productId = 1L, count = 50)),
                "batch-1",
            )
            rankingAggregationService.aggregate()

            // arrange - 2차 이벤트
            rankingEventService.saveViewBatch(
                listOf(ViewCount(productId = 1L, count = 30)),
                "batch-2",
            )

            // act - 2차 집계
            rankingAggregationService.aggregate()

            // assert
            val metric = rankingMetricJpaRepository.findByProductIdAndRankingDate(1L, today)!!
            assertThat(metric.totalScore).isEqualTo(0.1 * 50 + 0.1 * 30)

            val redisScore = masterRedisTemplate.opsForZSet().score(redisKey, "1")
            assertThat(redisScore).isEqualTo(0.1 * 80)
        }
    }

    @DisplayName("Redis 재구축")
    @Nested
    inner class RebuildRedis {
        @DisplayName("metric 테이블로부터 Redis ZSET을 재구축한다.")
        @Test
        fun rebuildsFromMetricTable() {
            // arrange - 이벤트 저장 + 집계
            rankingEventService.saveViewBatch(
                listOf(ViewCount(productId = 1L, count = 100)),
                "batch-1",
            )
            rankingAggregationService.aggregate()

            // Redis 초기화
            masterRedisTemplate.delete(redisKey)
            assertThat(masterRedisTemplate.opsForZSet().zCard(redisKey)).isEqualTo(0)

            // act - 재구축
            rankingAggregationService.rebuildRedis(today)

            // assert
            val score = masterRedisTemplate.opsForZSet().score(redisKey, "1")
            assertThat(score).isEqualTo(0.1 * 100)
        }
    }
}
