package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.ZonedDateTime

@DisplayName("CouponIssueService")
class CouponIssueServiceTest {

    private val couponIssueRepository: CouponIssueRepository = mockk()
    private val couponRepository: CouponRepository = mockk()
    private val couponIssueService = CouponIssueService(couponIssueRepository, couponRepository)

    companion object {
        private const val USER_ID = 1L
        private const val COUPON_ID = 10L
    }

    private fun createCoupon(
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): CouponModel = CouponModel(
        name = "테스트 쿠폰",
        type = CouponType.RATE,
        value = 10L,
        expiredAt = expiredAt,
    )

    @DisplayName("issue")
    @Nested
    inner class Issue {
        @DisplayName("유효한 쿠폰을 발급한다")
        @Test
        fun issuesCoupon_whenValid() {
            // arrange
            val coupon = createCoupon()
            every { couponRepository.findByIdAndDeletedAtIsNull(COUPON_ID) } returns coupon
            every {
                couponIssueRepository.findByCouponIdAndUserIdAndDeletedAtIsNull(COUPON_ID, USER_ID)
            } returns null
            every { couponIssueRepository.save(any()) } answers { firstArg() }

            // act
            val result = couponIssueService.issue(COUPON_ID, USER_ID)

            // assert
            assertThat(result.couponId).isEqualTo(COUPON_ID)
            assertThat(result.userId).isEqualTo(USER_ID)
            assertThat(result.status).isEqualTo(CouponIssueStatus.AVAILABLE)
            verify(exactly = 1) { couponIssueRepository.save(any()) }
        }

        @DisplayName("존재하지 않는 쿠폰을 발급하면 예외가 발생한다")
        @Test
        fun throwsException_whenCouponNotFound() {
            // arrange
            every { couponRepository.findByIdAndDeletedAtIsNull(COUPON_ID) } returns null

            // act & assert
            assertThatThrownBy { couponIssueService.issue(COUPON_ID, USER_ID) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
        }

        @DisplayName("만료된 쿠폰을 발급하면 예외가 발생한다")
        @Test
        fun throwsException_whenCouponExpired() {
            // arrange
            val expiredCoupon = createCoupon(expiredAt = ZonedDateTime.now().minusDays(1))
            every { couponRepository.findByIdAndDeletedAtIsNull(COUPON_ID) } returns expiredCoupon

            // act & assert
            assertThatThrownBy { couponIssueService.issue(COUPON_ID, USER_ID) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("만료된 쿠폰")
        }

        @DisplayName("이미 발급받은 쿠폰을 중복 발급하면 예외가 발생한다")
        @Test
        fun throwsException_whenAlreadyIssued() {
            // arrange
            val coupon = createCoupon()
            val existingIssue = CouponIssueModel(couponId = COUPON_ID, userId = USER_ID)
            every { couponRepository.findByIdAndDeletedAtIsNull(COUPON_ID) } returns coupon
            every {
                couponIssueRepository.findByCouponIdAndUserIdAndDeletedAtIsNull(COUPON_ID, USER_ID)
            } returns existingIssue

            // act & assert
            assertThatThrownBy { couponIssueService.issue(COUPON_ID, USER_ID) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CONFLICT)
        }
    }

    @DisplayName("findByUserId")
    @Nested
    inner class FindByUserId {
        @DisplayName("사용자의 발급 쿠폰 목록을 반환한다")
        @Test
        fun returnsUserCoupons() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val issues = listOf(CouponIssueModel(couponId = COUPON_ID, userId = USER_ID))
            every {
                couponIssueRepository.findAllByUserIdAndDeletedAtIsNull(USER_ID, pageable)
            } returns PageImpl(issues)

            // act
            val result = couponIssueService.findByUserId(USER_ID, pageable)

            // assert
            assertThat(result.content).hasSize(1)
        }
    }

    @DisplayName("findById")
    @Nested
    inner class FindById {
        @DisplayName("존재하는 발급 쿠폰을 반환한다")
        @Test
        fun returnsCouponIssue_whenExists() {
            // arrange
            val issue = CouponIssueModel(couponId = COUPON_ID, userId = USER_ID)
            every { couponIssueRepository.findByIdAndDeletedAtIsNull(1L) } returns issue

            // act
            val result = couponIssueService.findById(1L)

            // assert
            assertThat(result.couponId).isEqualTo(COUPON_ID)
        }

        @DisplayName("존재하지 않는 발급 쿠폰을 조회하면 예외가 발생한다")
        @Test
        fun throwsException_whenNotFound() {
            // arrange
            every { couponIssueRepository.findByIdAndDeletedAtIsNull(999L) } returns null

            // act & assert
            assertThatThrownBy { couponIssueService.findById(999L) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
        }
    }
}
