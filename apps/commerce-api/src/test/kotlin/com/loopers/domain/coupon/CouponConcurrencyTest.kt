package com.loopers.domain.coupon

import com.loopers.application.api.coupon.CouponFacade
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
class CouponConcurrencyTest @Autowired constructor(
    private val couponService: CouponService,
    private val couponRepository: CouponRepository,
    private val couponTemplateRepository: CouponTemplateRepository,
    private val couponFacade: CouponFacade,
    private val couponIssuanceResultRepository: CouponIssuanceResultRepository,
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

        val coupon = Coupon.issue(userId = 1L, template = savedTemplate, requestedAt = ZonedDateTime.now())
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

    @Test
    @DisplayName("totalCount=10인 쿠폰에 20명이 동시 발급 시 정확히 10개만 발급된다")
    fun testConcurrentIssuance_soldOutAfter10() {
        // Arrange: totalCount=10 쿠폰 생성
        val totalCount = 10
        val totalRequests = 20
        val template = CouponTemplate.create(
            name = "선착순 100장",
            type = CouponType.FIXED,
            value = BigDecimal("1000"),
            minOrderAmount = BigDecimal("5000"),
            expiredAt = ZonedDateTime.now().plusDays(30),
            totalCount = totalCount,
        )
        val savedTemplate = couponTemplateRepository.save(template)

        // Act: 200개 스레드 동시 발급 요청 (서로 다른 userId)
        val latch = CountDownLatch(totalRequests)
        val executor = Executors.newFixedThreadPool(totalRequests)
        val results = Collections.synchronizedList(mutableListOf<Result<Coupon>>())

        val futures = (1..totalRequests).map { userId ->
            executor.submit<Unit> {
                latch.countDown()
                latch.await()
                results.add(runCatching { couponService.issueCoupon(userId.toLong(), savedTemplate.id) })
            }
        }
        futures.forEach { it.get(30, TimeUnit.SECONDS) }
        executor.shutdown()
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            executor.shutdownNow()
            throw AssertionError("Executor did not terminate within 30 seconds")
        }

        // Assert: 발급 결과 검증
        val successCount = results.count { it.isSuccess }
        val failureCount = results.count { it.isFailure }
        val soldOutCount = results.count {
            it.isFailure && it.exceptionOrNull()?.message?.contains("소진") == true
        }

        println("Success: $successCount, Failure: $failureCount, SoldOut: $soldOutCount")
        results.filter { it.isFailure }.take(5).forEach {
            println("Error: ${it.exceptionOrNull()?.message}")
        }

        // 정확히 100개만 성공
        assertThat(successCount)
            .withFailMessage("Expected 100 successful issuances, but got $successCount")
            .isEqualTo(totalCount)
        // 100개 실패
        assertThat(failureCount).isEqualTo(totalRequests - totalCount)
        // 모두 "소진" 예외
        assertThat(soldOutCount).isEqualTo(totalRequests - totalCount)

        // DB 검증: 정확히 100개의 쿠폰만 존재
        val issuedCoupons = couponRepository.findByTemplateId(
            savedTemplate.id,
            org.springframework.data.domain.PageRequest.of(0, 500),
        )
        assertThat(issuedCoupons.totalElements).isEqualTo(totalCount.toLong())

        // 템플릿의 issuedCount도 100으로 업데이트되어야 함
        val updatedTemplate = couponTemplateRepository.findById(savedTemplate.id)
        assertThat(updatedTemplate?.issuedCount).isEqualTo(totalCount)
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

    @DisplayName("쿠폰 발급 요청 - Async 처리")
    @Nested
    inner class AsyncIssuanceRequest {

        @DisplayName("발급 요청 시 PENDING 상태로 CouponIssuanceResult가 저장된다")
        @Test
        fun testRequestIssuance_SavesPending() {
            // arrange
            val template = CouponTemplate.create(
                name = "비동기 발급 테스트",
                type = CouponType.FIXED,
                value = BigDecimal("1000"),
                minOrderAmount = BigDecimal("5000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )
            val savedTemplate = couponTemplateRepository.save(template)

            // act
            val result = couponFacade.requestIssuance(userId = 100L, templateId = savedTemplate.id)

            // assert
            assertThat(result.dedupeKey).isNotEmpty()

            // ✅ CouponIssuanceResult가 PENDING으로 저장됨
            val issuanceResult = couponIssuanceResultRepository.findByDedupeKey(result.dedupeKey)
            assertThat(issuanceResult).isNotNull
            assertThat(issuanceResult!!.status).isEqualTo(IssuanceStatus.PENDING)
            assertThat(issuanceResult.userId).isEqualTo(100L)
            assertThat(issuanceResult.templateId).isEqualTo(savedTemplate.id)
            assertThat(issuanceResult.couponId).isNull()
        }

        @DisplayName("발급 요청 상태 조회 API 검증")
        @Test
        fun testGetIssuanceStatus() {
            // arrange
            val template = CouponTemplate.create(
                name = "상태 조회 테스트",
                type = CouponType.FIXED,
                value = BigDecimal("1000"),
                minOrderAmount = BigDecimal("5000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )
            val savedTemplate = couponTemplateRepository.save(template)
            val userId = 200L

            // act: 발급 요청
            val requestResult = couponFacade.requestIssuance(userId, savedTemplate.id)

            // act: 상태 조회
            val statusInfo = couponFacade.getIssuanceStatus(requestResult.dedupeKey, userId)

            // assert
            assertThat(statusInfo.dedupeKey).isEqualTo(requestResult.dedupeKey)
            assertThat(statusInfo.status).isEqualTo(IssuanceStatus.PENDING)
            assertThat(statusInfo.couponId).isNull()
            assertThat(statusInfo.createdAt).isNotNull()
        }

        @DisplayName("다른 사용자의 발급 요청은 조회할 수 없다")
        @Test
        fun testGetIssuanceStatus_ForbiddenForOtherUser() {
            // arrange
            val template = CouponTemplate.create(
                name = "권한 테스트",
                type = CouponType.FIXED,
                value = BigDecimal("1000"),
                minOrderAmount = BigDecimal("5000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )
            val savedTemplate = couponTemplateRepository.save(template)

            val requestResult = couponFacade.requestIssuance(userId = 300L, templateId = savedTemplate.id)

            // act & assert: 다른 사용자(400L)가 조회 시도
            assertThatThrownBy {
                couponFacade.getIssuanceStatus(requestResult.dedupeKey, userId = 400L)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.FORBIDDEN)
        }

        @DisplayName("존재하지 않는 발급 요청을 조회하면 404 반환")
        @Test
        fun testGetIssuanceStatus_NotFound() {
            // act & assert
            assertThatThrownBy {
                couponFacade.getIssuanceStatus("non-existent-dedupeKey", userId = 100L)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
        }

        @DisplayName("여러 발급 요청이 각각 고유한 결과를 생성한다")
        @Test
        fun testRequestIssuance_Concurrent_UniqueDedupKeys() {
            // arrange
            val template = CouponTemplate.create(
                name = "동시 요청 dedupeKey 테스트",
                type = CouponType.FIXED,
                value = BigDecimal("1000"),
                minOrderAmount = BigDecimal("5000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )
            val savedTemplate = couponTemplateRepository.save(template)

            val requestCount = 20
            val threadCount = 5
            val barrier = CyclicBarrier(threadCount)
            val executor = Executors.newFixedThreadPool(threadCount)
            val dedupKeys = Collections.synchronizedSet(mutableSetOf<String>())

            // act: 20명이 최대 5개 스레드로 처리 (배치 처리)
            val tasks = (1..requestCount).map { userId ->
                executor.submit {
                    try {
                        barrier.await() // 각 배치가 동시에 시작되도록 동기화
                    } catch (e: Exception) {
                        // ignore
                    }

                    val result = couponFacade.requestIssuance(userId.toLong(), savedTemplate.id)
                    dedupKeys.add(result.dedupeKey)
                }
            }

            tasks.forEach { task ->
                try {
                    task.get(10, TimeUnit.SECONDS)
                } catch (e: java.util.concurrent.TimeoutException) {
                    throw AssertionError("Task timeout: ${e.message}")
                }
            }
            executor.shutdown()

            // assert
            // ✅ 20개의 고유한 dedupeKey
            assertThat(dedupKeys).hasSize(requestCount)

            // ✅ 모든 결과가 PENDING 상태
            dedupKeys.forEach { dedupeKey ->
                val result = couponIssuanceResultRepository.findByDedupeKey(dedupeKey)
                assertThat(result).isNotNull
                assertThat(result!!.status).isEqualTo(IssuanceStatus.PENDING)
            }
        }
    }
}
