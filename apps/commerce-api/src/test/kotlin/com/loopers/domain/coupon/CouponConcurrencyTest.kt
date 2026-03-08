package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.orm.ObjectOptimisticLockingFailureException
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
class CouponConcurrencyTest @Autowired constructor(
    private val couponService: CouponService,
    private val couponRepository: CouponRepository,
    private val couponTemplateRepository: CouponTemplateRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("같은 (userId, templateId)로 동시에 쿠폰 발급 시 1개만 성공 (DB unique constraint)")
    fun testConcurrentCouponIssuance() {
        // Arrange
        val userId = 100L
        val template = CouponTemplate.create(
            name = "발급 동시성 테스트",
            type = CouponType.FIXED,
            value = BigDecimal("1000"),
            minOrderAmount = BigDecimal("5000"),
            expiredAt = ZonedDateTime.now().plusDays(30),
        )
        val savedTemplate = couponTemplateRepository.save(template)

        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        val successCount = Collections.synchronizedList(mutableListOf<Long>())
        val failCount = Collections.synchronizedList(mutableListOf<String>())

        // Act: 모든 스레드가 동시에 같은 (userId, templateId)로 발급 시도
        val tasks = (1..threadCount).map {
            executor.submit {
                latch.countDown()
                latch.await()
                try {
                    val coupon = couponService.issueCoupon(userId, savedTemplate.id)
                    successCount.add(coupon.id)
                } catch (e: CoreException) {
                    failCount.add("DUPLICATE")
                } catch (e: Exception) {
                    failCount.add(e.javaClass.simpleName)
                }
            }
        }

        tasks.forEach { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        // Assert: 정확히 1개만 성공, 9개는 실패
        assertThat(successCount).hasSize(1)
        assertThat(failCount).hasSize(threadCount - 1)

        // DB에는 정확히 1개의 쿠폰만 존재
        val coupons = couponRepository.findByUserId(userId, org.springframework.data.domain.PageRequest.of(0, 100)).content
        assertThat(coupons).hasSize(1)
        assertThat(coupons[0].userId).isEqualTo(userId)
        assertThat(coupons[0].templateId).isEqualTo(savedTemplate.id)
    }

    @Test
    @DisplayName("같은 쿠폰을 동시에 사용할 때 낙관락으로 방지한다 (10개 스레드)")
    fun testConcurrentCouponUsage() {
        // Arrange
        val template = CouponTemplate.create(
            name = "동시성 테스트 쿠폰",
            type = CouponType.FIXED,
            value = BigDecimal("1000"),
            minOrderAmount = BigDecimal("5000"),
            expiredAt = ZonedDateTime.now().plusDays(30),
        )
        val savedTemplate = couponTemplateRepository.save(template)

        val coupon = Coupon.issue(userId = 1L, template = savedTemplate)
        val savedCoupon = couponRepository.save(coupon)

        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        val results = Collections.synchronizedList(mutableListOf<CouponUsageResult>())

        // Act: 10개 스레드가 동시에 같은 쿠폰 사용
        val tasks = (1..threadCount).map {
            executor.submit {
                latch.countDown()
                latch.await()

                try {
                    couponService.useCoupon(1L, savedCoupon.id, BigDecimal("10000"))
                    results.add(CouponUsageResult.Success)
                } catch (e: ObjectOptimisticLockingFailureException) {
                    results.add(CouponUsageResult.OptimisticLockFailed)
                } catch (e: CoreException) {
                    results.add(CouponUsageResult.AlreadyUsed)
                } catch (e: Exception) {
                    results.add(CouponUsageResult.Failure(e.javaClass.simpleName))
                }
            }
        }

        tasks.forEach { task ->
            try {
                task.get(10, TimeUnit.SECONDS)
            } catch (e: java.util.concurrent.TimeoutException) {
                throw AssertionError("Task timeout after 10 seconds")
            }
        }
        executor.shutdown()
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            executor.shutdownNow()
            throw AssertionError("Executor did not terminate within 10 seconds")
        }

        // Assert
        val successCount = results.count { it is CouponUsageResult.Success }
        val optimisticLockFailureCount = results.count { it is CouponUsageResult.OptimisticLockFailed }
        val alreadyUsedCount = results.count { it is CouponUsageResult.AlreadyUsed }

        // 최대 1개만 성공해야 함 (첫 번째 성공한 스레드)
        assertThat(successCount).isEqualTo(1)

        // 나머지는 낙관락 실패 또는 이미 사용됨
        assertThat(optimisticLockFailureCount + alreadyUsedCount).isEqualTo(threadCount - 1)

        // 최종 쿠폰 상태 확인
        val finalCoupon = couponRepository.findById(savedCoupon.id)
        assertThat(finalCoupon?.status).isEqualTo(CouponStatus.USED)
        assertThat(finalCoupon?.version).isGreaterThan(0)
    }

    @Test
    @DisplayName("쿠폰 사용 시 버전이 증가한다")
    fun testCouponVersionIncrementOnUsage() {
        // Arrange
        val template = CouponTemplate.create(
            name = "버전 테스트 쿠폰",
            type = CouponType.FIXED,
            value = BigDecimal("1000"),
            minOrderAmount = BigDecimal("5000"),
            expiredAt = ZonedDateTime.now().plusDays(30),
        )
        val savedTemplate = couponTemplateRepository.save(template)

        val coupon = Coupon.issue(userId = 2L, template = savedTemplate)
        val savedCoupon = couponRepository.save(coupon)

        val initialVersion = savedCoupon.version

        // Act
        couponService.useCoupon(2L, savedCoupon.id, BigDecimal("10000"))

        // Assert
        val updatedCoupon = couponRepository.findById(savedCoupon.id)
        assertThat(updatedCoupon?.version).isGreaterThan(initialVersion)
        assertThat(updatedCoupon?.status).isEqualTo(CouponStatus.USED)
    }

    sealed class CouponUsageResult {
        object Success : CouponUsageResult()
        object OptimisticLockFailed : CouponUsageResult()
        object AlreadyUsed : CouponUsageResult()
        data class Failure(val reason: String) : CouponUsageResult()
    }
}
