package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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
    @DisplayName("같은 (userId, templateId)로 쿠폰 발급 시 중복 발급 방지 (순차 처리)")
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

        val attemptCount = 3
        val results = mutableListOf<CouponIssuanceResult>()

        // Act: 같은 (userId, templateId)로 여러 번 발급 시도 (순차)
        repeat(attemptCount) {
            try {
                val coupon = couponService.issueCoupon(userId, savedTemplate.id)
                results.add(CouponIssuanceResult.Success(coupon.id))
            } catch (e: CoreException) {
                results.add(CouponIssuanceResult.DuplicateError(e.errorType, e.message))
            } catch (e: Exception) {
                results.add(CouponIssuanceResult.InfraError(e.javaClass.simpleName, e.message))
            }
        }

        // Assert: 정확히 1개만 성공, 2개는 중복 예외
        val successCount = results.filterIsInstance<CouponIssuanceResult.Success>()
        val duplicateErrors = results.filterIsInstance<CouponIssuanceResult.DuplicateError>()
        val infraErrors = results.filterIsInstance<CouponIssuanceResult.InfraError>()

        assertThat(infraErrors).isEmpty() // 원본 예외 없어야 함
        assertThat(successCount).hasSize(1)
        assertThat(duplicateErrors).hasSize(attemptCount - 1)

        // 모든 중복 오류가 올바른 도메인 예외인지 확인
        duplicateErrors.forEach { error ->
            assertThat(error.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(error.message).contains("중복")
        }

        // DB에는 정확히 1개의 쿠폰만 존재
        val coupons = couponRepository.findByUserId(userId, org.springframework.data.domain.PageRequest.of(0, 100)).content
        assertThat(coupons).hasSize(1)
        assertThat(coupons[0].userId).isEqualTo(userId)
        assertThat(coupons[0].templateId).isEqualTo(savedTemplate.id)
    }

    @Test
    @DisplayName("같은 쿠폰을 동시에 사용할 때 원자 업데이트로 방지한다 (10개 스레드)")
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
        val alreadyUsedCount = results.count { it is CouponUsageResult.AlreadyUsed }
        val failureCount = results.count { it is CouponUsageResult.Failure }

        // 최대 1개만 성공해야 함 (원자 업데이트로 인해)
        assertThat(successCount).isEqualTo(1)

        // 나머지는 모두 "이미 사용됨" 예외
        assertThat(alreadyUsedCount).isEqualTo(threadCount - 1)

        // 예기치 않은 실패가 없어야 함
        assertThat(failureCount).isEqualTo(0)

        // 최종 쿠폰 상태 확인
        val finalCoupon = couponRepository.findById(savedCoupon.id)
        assertThat(finalCoupon?.status).isEqualTo(CouponStatus.USED)
    }

    sealed class CouponIssuanceResult {
        data class Success(val couponId: Long) : CouponIssuanceResult()
        data class DuplicateError(val errorType: ErrorType, val message: String?) : CouponIssuanceResult()
        data class InfraError(val exceptionType: String, val message: String?) : CouponIssuanceResult()
    }

    sealed class CouponUsageResult {
        object Success : CouponUsageResult()
        object AlreadyUsed : CouponUsageResult()
        data class Failure(val reason: String) : CouponUsageResult()
    }
}
