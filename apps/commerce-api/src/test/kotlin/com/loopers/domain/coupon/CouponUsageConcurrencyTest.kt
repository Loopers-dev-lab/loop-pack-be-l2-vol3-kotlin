package com.loopers.domain.coupon

import com.loopers.infrastructure.coupon.CouponEntity
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponEntity
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class CouponUsageConcurrencyTest @Autowired constructor(
    private val issuedCouponRepository: IssuedCouponRepository,
    private val issuedCouponReader: IssuedCouponReader,
    private val couponJpaRepository: CouponJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val transactionManager: PlatformTransactionManager,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    fun `동시에_같은_쿠폰으로_사용하면_하나만_성공한다`() {
        // arrange
        val couponEntity = couponJpaRepository.save(
            CouponEntity(
                name = "테스트쿠폰",
                type = CouponType.FIXED.name,
                discountValue = 3000L,
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        )
        val issuedEntity = issuedCouponJpaRepository.save(
            IssuedCouponEntity(
                couponId = couponEntity.id!!,
                memberId = 1L,
                status = CouponStatus.AVAILABLE.name,
                issuedAt = ZonedDateTime.now(),
            ),
        )

        val threadCount = 10
        val executorService = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val transactionTemplate = TransactionTemplate(transactionManager)

        // act
        repeat(threadCount) {
            executorService.submit {
                try {
                    transactionTemplate.execute {
                        val issuedCoupon = issuedCouponReader.getByIdForUpdate(issuedEntity.id!!)
                        issuedCoupon.use()
                        issuedCouponRepository.save(issuedCoupon)
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
        executorService.shutdown()

        // assert
        assertThat(successCount.get()).isEqualTo(1)
        assertThat(failCount.get()).isEqualTo(9)

        val updatedEntity = issuedCouponJpaRepository.findById(issuedEntity.id!!).get()
        assertThat(updatedEntity.status).isEqualTo(CouponStatus.USED.name)
    }
}
