package com.loopers.application.metrics

import com.loopers.config.redis.RedisRankingConstants
import com.loopers.domain.ranking.RankingWeight
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * M-3: afterCommit 동작 검증 — 실제 트랜잭션 커밋/롤백 시나리오
 *
 * - 커밋 시나리오: UseCase 자체 트랜잭션 커밋 → afterCommit 실행 → Redis 갱신
 * - 롤백 시나리오: TransactionTemplate + setRollbackOnly → afterCommit 미실행 → Redis 미갱신
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UpdateProductMetricsAfterCommitTest @Autowired constructor(
    private val useCase: UpdateProductMetricsUseCase,
    private val redisTemplate: RedisTemplate<String, String>,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
    transactionManager: PlatformTransactionManager,
) {

    private val txTemplate = TransactionTemplate(transactionManager)

    private val rankingKey: String
        get() {
            val today = LocalDate.now(RedisRankingConstants.KST_ZONE)
            return "${RedisRankingConstants.RANKING_KEY_PREFIX}${today.format(DateTimeFormatter.BASIC_ISO_DATE)}"
        }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("트랜잭션 커밋 시")
    inner class OnCommit {

        @Test
        @DisplayName("DB 커밋 후 Redis 랭킹 점수가 갱신된다")
        fun `커밋 성공 시 Redis 점수 반영`() {
            // Act
            useCase.handleCatalogEvent("evt-1", UpdateProductMetricsUseCase.PRODUCT_VIEWED, 1L)

            // Assert — Redis에 점수 반영됨
            val score = redisTemplate.opsForZSet().score(rankingKey, "1")
            assertThat(score).isNotNull()
            assertThat(score).isCloseTo(RankingWeight.VIEW, Offset.offset(0.001))
        }

        @Test
        @DisplayName("주문 이벤트도 커밋 후 Redis에 반영된다")
        fun `주문 이벤트 커밋 성공 시 Redis 점수 반영`() {
            // Act
            useCase.handleOrderEvent(
                "evt-1",
                UpdateProductMetricsUseCase.PAYMENT_COMPLETED,
                1L,
                3L,
            )

            // Assert
            val score = redisTemplate.opsForZSet().score(rankingKey, "1")
            assertThat(score).isNotNull()
            assertThat(score).isCloseTo(RankingWeight.ORDER * 3, Offset.offset(0.001))
        }
    }

    @Nested
    @DisplayName("트랜잭션 롤백 시")
    inner class OnRollback {

        @Test
        @DisplayName("DB 롤백 시 Redis 랭킹 점수가 갱신되지 않는다")
        fun `롤백 시 Redis 미갱신`() {
            // Act — 외부 TransactionTemplate으로 감싸서 rollback 강제
            txTemplate.execute { status ->
                useCase.handleCatalogEvent("evt-1", UpdateProductMetricsUseCase.PRODUCT_VIEWED, 1L)
                status.setRollbackOnly()
            }

            // Assert — 롤백되어 afterCommit 미실행 → Redis에 반영 없음
            val score: Any? = redisTemplate.opsForZSet().score(rankingKey, "1")
            assertThat(score).isNull()
        }

        @Test
        @DisplayName("주문 이벤트도 롤백 시 Redis에 반영되지 않는다")
        fun `주문 이벤트 롤백 시 Redis 미갱신`() {
            // Act
            txTemplate.execute { status ->
                useCase.handleOrderEvent(
                    "evt-1",
                    UpdateProductMetricsUseCase.PAYMENT_COMPLETED,
                    1L,
                    2L,
                )
                status.setRollbackOnly()
            }

            // Assert
            val score: Any? = redisTemplate.opsForZSet().score(rankingKey, "1")
            assertThat(score).isNull()
        }
    }
}
