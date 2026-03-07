package com.loopers.concurrency

import com.loopers.domain.Money
import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponIssue
import com.loopers.domain.coupon.CouponIssueStatus
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.CouponType
import com.loopers.infrastructure.coupon.CouponIssueJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.orm.ObjectOptimisticLockingFailureException
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class CouponConcurrencyTest @Autowired constructor(
    private val couponService: CouponService,
    private val couponJpaRepository: CouponJpaRepository,
    private val couponIssueJpaRepository: CouponIssueJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    /**
     * 쿠폰 사용 동시성 테스트 — @Version 낙관적 락.
     *
     * [문제] @Version이 없으면?
     * - 20개 스레드가 동시에 CouponIssue를 읽으면 모두 status=AVAILABLE, version 체크 없음
     * - Hibernate dirty checking: UPDATE SET status='USED' WHERE id=?
     * - 모든 스레드의 UPDATE가 성공 → 20건 모두 "쿠폰 사용 성공" → 중복 사용 버그
     *
     * [해결] @Version을 추가하면?
     * - Hibernate dirty checking: UPDATE SET status='USED', version=1 WHERE id=? AND version=0
     * - 첫 번째 커밋만 version=0 조건에 매칭 → 성공
     * - 나머지 19개는 version 불일치 → ObjectOptimisticLockingFailureException
     */
    @Nested
    @DisplayName("쿠폰 사용 동시성 — @Version 낙관적 락")
    inner class UseCoupon {

        @Test
        @Timeout(10, unit = TimeUnit.SECONDS)
        @DisplayName("20개 스레드가 동시에 같은 쿠폰을 사용하면, 1건만 성공하고 나머지는 낙관적 락 충돌로 실패한다")
        fun optimisticLockPreventsDuplicateUse() {
            // arrange
            val coupon = couponJpaRepository.save(
                Coupon(
                    name = "테스트 쿠폰",
                    type = CouponType.FIXED,
                    value = 5000,
                    expiredAt = ZonedDateTime.now().plusDays(7),
                ),
            )
            val userId = 1L
            val couponIssue = couponIssueJpaRepository.save(
                CouponIssue(couponId = coupon.id, userId = userId),
            )
            val orderAmount = Money(50000)

            val threadCount = 20
            val executorService = Executors.newFixedThreadPool(threadCount)
            val startLatch = CountDownLatch(1)
            val endLatch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val optimisticLockFailCount = AtomicInteger(0)
            val otherFailCount = AtomicInteger(0)

            // act
            repeat(threadCount) {
                executorService.submit {
                    try {
                        startLatch.await()
                        couponService.useCouponForOrder(couponIssue.id, userId, orderAmount)
                        successCount.incrementAndGet()
                    } catch (e: ObjectOptimisticLockingFailureException) {
                        optimisticLockFailCount.incrementAndGet()
                    } catch (e: Exception) {
                        otherFailCount.incrementAndGet()
                    } finally {
                        endLatch.countDown()
                    }
                }
            }
            startLatch.countDown()
            endLatch.await()
            executorService.shutdown()

            // assert
            val updatedIssue = couponIssueJpaRepository.findById(couponIssue.id).get()
            val totalFail = optimisticLockFailCount.get() + otherFailCount.get()
            assertAll(
                // 정확히 1건만 성공
                { assertThat(successCount.get()).isEqualTo(1) },
                // 나머지 19건은 실패 (낙관적 락 충돌 or 이미 사용된 쿠폰)
                { assertThat(totalFail).isEqualTo(threadCount - 1) },
                // 최종 상태는 USED
                { assertThat(updatedIssue.status).isEqualTo(CouponIssueStatus.USED) },
                // version이 1로 증가 (최초 0 → 사용 후 1)
                { assertThat(updatedIssue.version).isEqualTo(1L) },
            )
        }
    }
}
