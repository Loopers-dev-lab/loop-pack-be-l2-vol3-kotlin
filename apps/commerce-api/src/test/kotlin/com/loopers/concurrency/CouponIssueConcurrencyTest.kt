package com.loopers.concurrency

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("Coupon issue concurrency")
@SpringBootTest
class CouponIssueConcurrencyTest
@Autowired
constructor(
    private val couponRepository: CouponRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    platformTransactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(platformTransactionManager)

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("동일 쿠폰을 2스레드가 동시에 발급해도 1건만 성공한다")
    fun issue_concurrent_onlyOneSucceeds() {
        val coupon = couponRepository.save(
            Coupon.register(
                name = "선착순 쿠폰",
                type = Coupon.Type.FIXED,
                discountValue = 1000L,
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
                issueLimit = 1L,
            ),
        )

        val executor = Executors.newFixedThreadPool(2)
        val latch = CountDownLatch(2)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        repeat(2) {
            executor.submit {
                try {
                    transactionTemplate.executeWithoutResult {
                        val locked = couponRepository.findByIdForUpdate(coupon.id!!)
                        val issued = locked?.issue() ?: error("coupon missing")
                        couponRepository.save(issued)
                    }
                    successCount.incrementAndGet()
                } catch (_: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        executor.shutdown()

        val savedCoupon = couponRepository.findById(coupon.id!!)!!

        assertThat(successCount.get()).isEqualTo(1)
        assertThat(failCount.get()).isEqualTo(1)
        assertThat(savedCoupon.issuedCount).isEqualTo(1L)
    }
}
