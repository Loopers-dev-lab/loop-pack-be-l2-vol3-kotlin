package com.loopers.concurrency

import com.loopers.application.coupon.CouponCommand
import com.loopers.application.coupon.RegisterCouponUseCase
import com.loopers.application.coupon.RequestCouponIssueAsyncUseCase
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.application.user.UserCommand
import com.loopers.domain.coupon.CouponIssueStatus
import com.loopers.domain.coupon.CouponType
import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.CouponErrorCode
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDate
import java.time.ZonedDateTime

@SpringBootTest
@Import(MySqlTestContainersConfig::class, RedisTestContainersConfig::class)
class CouponIssueAsyncConcurrencyTest @Autowired constructor(
    private val requestCouponIssueAsyncUseCase: RequestCouponIssueAsyncUseCase,
    private val registerCouponUseCase: RegisterCouponUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val userJpaRepository: UserJpaRepository,
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun registerUser(loginId: String): Long {
        registerUserUseCase.execute(
            UserCommand.Register(
                loginId = loginId,
                rawPassword = "Test123!",
                name = "테스트",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "$loginId@example.com",
            ),
        )
        return userJpaRepository.findByLoginId(loginId)!!.id
    }

    private fun registerLimitedCoupon(maxQuantity: Int): Long {
        return registerCouponUseCase.execute(
            CouponCommand.Register(
                name = "선착순쿠폰",
                type = CouponType.FIXED,
                value = 5000,
                minOrderAmount = null,
                maxQuantity = maxQuantity,
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        ).id
    }

    @DisplayName("100명이 동시에 10장짜리 쿠폰 비동기 발급 요청 시, Redis INCR 게이트에서 정확히 10건만 통과하고 90건은 SOLD_OUT 처리된다")
    @Test
    fun exactlyMaxQuantityShouldPassRedisGateUnderConcurrentRequests() {
        // arrange
        val maxQuantity = 10
        val threadCount = 100
        val userIds = (1..threadCount).map { registerUser("user$it") }
        val couponId = registerLimitedCoupon(maxQuantity)

        // act
        val actions = userIds.map { userId ->
            {
                requestCouponIssueAsyncUseCase.execute(
                    CouponCommand.IssueAsync(couponId = couponId, userId = userId),
                )
            }
        }
        val results = ConcurrencyTestHelper.executeConcurrently(actions)

        val successes = results.filter { it.isSuccess }
        val failures = results.filter { it.isFailure }

        // assert
        val pendingRequests = couponIssueRequestJpaRepository.findAll()
            .filter { it.status == CouponIssueStatus.PENDING }

        val redisCounter = redisTemplate.opsForValue().get("coupon:$couponId:counter")?.toLong() ?: 0L

        assertAll(
            {
                assertThat(successes).`as`("Redis INCR 게이트를 통과한 요청은 정확히 $maxQuantity 건이어야 한다")
                    .hasSize(maxQuantity)
            },
            {
                assertThat(failures).`as`("SOLD_OUT으로 거절된 요청은 ${threadCount - maxQuantity}건이어야 한다")
                    .hasSize(threadCount - maxQuantity)
            },
            {
                assertThat(failures).`as`("실패 원인은 모두 COUPON_SOLD_OUT이어야 한다")
                    .allSatisfy { result ->
                        val exception = result.exceptionOrNull() as CoreException
                        assertThat(exception.errorCode).isEqualTo(CouponErrorCode.COUPON_SOLD_OUT)
                    }
            },
            {
                assertThat(pendingRequests).`as`("DB에 PENDING 상태 요청이 정확히 $maxQuantity 건이어야 한다")
                    .hasSize(maxQuantity)
            },
            {
                assertThat(redisCounter).`as`("Redis 카운터가 DECR 보정되어 정확히 $maxQuantity 이어야 한다")
                    .isEqualTo(maxQuantity.toLong())
            },
        )
    }

    @DisplayName("수량 초과 요청의 DECR 보정 후 Redis 카운터가 maxQuantity와 정확히 일치해야 한다")
    @Test
    fun redisCounterShouldBeCorrectedAfterSoldOutRejections() {
        // arrange — 소규모로 카운터 정확도만 검증
        val maxQuantity = 3
        val threadCount = 20
        val userIds = (1..threadCount).map { registerUser("counteruser$it") }
        val couponId = registerLimitedCoupon(maxQuantity)

        // act
        val actions = userIds.map { userId ->
            {
                requestCouponIssueAsyncUseCase.execute(
                    CouponCommand.IssueAsync(couponId = couponId, userId = userId),
                )
            }
        }
        val results = ConcurrencyTestHelper.executeConcurrently(actions)

        val successes = results.filter { it.isSuccess }
        val redisCounter = redisTemplate.opsForValue().get("coupon:$couponId:counter")?.toLong() ?: 0L

        // assert — SOLD_OUT 거절 시 DECR이 정확히 동작했는지 검증
        assertAll(
            {
                assertThat(successes).`as`("통과 건수가 maxQuantity와 일치해야 한다")
                    .hasSize(maxQuantity)
            },
            {
                assertThat(redisCounter).`as`("DECR 보정 후 Redis 카운터가 maxQuantity와 일치해야 한다")
                    .isEqualTo(maxQuantity.toLong())
            },
        )
    }
}
