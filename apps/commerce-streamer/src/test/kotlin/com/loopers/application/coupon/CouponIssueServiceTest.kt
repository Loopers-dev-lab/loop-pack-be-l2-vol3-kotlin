package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponInfo
import com.loopers.domain.coupon.CouponIssuanceRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class CouponIssueServiceTest {

    @Mock
    private lateinit var couponIssuanceRepository: CouponIssuanceRepository

    @InjectMocks
    private lateinit var couponIssueService: CouponIssueService

    private fun createCouponInfo(
        id: Long = 1L,
        maxIssueCount: Int? = 100,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(7),
        deletedAt: ZonedDateTime? = null,
    ): CouponInfo {
        val coupon = CouponInfo()
        ReflectionTestUtils.setField(coupon, "id", id)
        ReflectionTestUtils.setField(coupon, "maxIssueCount", maxIssueCount)
        ReflectionTestUtils.setField(coupon, "expiredAt", expiredAt)
        ReflectionTestUtils.setField(coupon, "deletedAt", deletedAt)
        return coupon
    }

    @DisplayName("쿠폰을 발급할 때,")
    @Nested
    inner class Issue {

        @DisplayName("정상 요청이면, 발급 성공한다.")
        @Test
        fun issuesSuccessfully_whenValidRequest() {
            // arrange
            val coupon = createCouponInfo()
            whenever(couponIssuanceRepository.findCouponById(1L)).thenReturn(coupon)
            whenever(couponIssuanceRepository.existsByCouponIdAndUserId(1L, 1L)).thenReturn(false)
            whenever(couponIssuanceRepository.countByCouponId(1L)).thenReturn(0L)
            whenever(couponIssuanceRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            val result = couponIssueService.issue(1L, 1L)

            // assert
            assertThat(result.success).isTrue()
            verify(couponIssuanceRepository).save(any())
        }

        @DisplayName("쿠폰이 없으면, 실패한다.")
        @Test
        fun fails_whenCouponNotFound() {
            // arrange
            whenever(couponIssuanceRepository.findCouponById(999L)).thenReturn(null)

            // act
            val result = couponIssueService.issue(999L, 1L)

            // assert
            assertThat(result.success).isFalse()
            assertThat(result.failureReason).contains("찾을 수 없습니다")
            verify(couponIssuanceRepository, never()).save(any())
        }

        @DisplayName("이미 발급받았으면, 실패한다.")
        @Test
        fun fails_whenAlreadyIssued() {
            // arrange
            val coupon = createCouponInfo()
            whenever(couponIssuanceRepository.findCouponById(1L)).thenReturn(coupon)
            whenever(couponIssuanceRepository.existsByCouponIdAndUserId(1L, 1L)).thenReturn(true)

            // act
            val result = couponIssueService.issue(1L, 1L)

            // assert
            assertThat(result.success).isFalse()
            assertThat(result.failureReason).contains("이미 발급")
            verify(couponIssuanceRepository, never()).save(any())
        }

        @DisplayName("수량이 소진되면, 실패한다.")
        @Test
        fun fails_whenQuantityExhausted() {
            // arrange
            val coupon = createCouponInfo(maxIssueCount = 100)
            whenever(couponIssuanceRepository.findCouponById(1L)).thenReturn(coupon)
            whenever(couponIssuanceRepository.existsByCouponIdAndUserId(1L, 1L)).thenReturn(false)
            whenever(couponIssuanceRepository.countByCouponId(1L)).thenReturn(100L)

            // act
            val result = couponIssueService.issue(1L, 1L)

            // assert
            assertThat(result.success).isFalse()
            assertThat(result.failureReason).contains("수량이 소진")
            verify(couponIssuanceRepository, never()).save(any())
        }

        @DisplayName("만료된 쿠폰이면, 실패한다.")
        @Test
        fun fails_whenExpired() {
            // arrange
            val coupon = createCouponInfo(expiredAt = ZonedDateTime.now().minusDays(1))
            whenever(couponIssuanceRepository.findCouponById(1L)).thenReturn(coupon)

            // act
            val result = couponIssueService.issue(1L, 1L)

            // assert
            assertThat(result.success).isFalse()
            assertThat(result.failureReason).contains("만료")
        }
    }
}
