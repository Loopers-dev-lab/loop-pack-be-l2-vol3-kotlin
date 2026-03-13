package com.loopers.concurrency

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("IssuedCoupon 동시성 테스트")
@SpringBootTest
class IssuedCouponConcurrencyTest
@Autowired
constructor(
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val txManager: PlatformTransactionManager,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("동일 쿠폰을 10스레드가 동시에 사용 → 1건만 성공, 나머지 실패")
    fun use_concurrent_onlyOneSucceeds() {
        val coupon = couponRepository.save(
            Coupon.register(
                name = "동시성 테스트 쿠폰",
                type = Coupon.Type.FIXED,
                discountValue = 1000,
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        )
        val issuedCoupon = issuedCouponRepository.save(
            IssuedCoupon.issue(
                couponId = coupon.id!!,
                userId = 1L,
                expiredAt = coupon.expiredAt,
            ),
        )

        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val txTemplate = TransactionTemplate(txManager)

        repeat(threadCount) {
            executor.submit {
                try {
                    txTemplate.execute {
                        val used = issuedCoupon.use()
                        issuedCouponRepository.use(used)
                    }
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        assertThat(successCount.get()).isEqualTo(1)
        assertThat(failCount.get()).isEqualTo(threadCount - 1)
    }
}
