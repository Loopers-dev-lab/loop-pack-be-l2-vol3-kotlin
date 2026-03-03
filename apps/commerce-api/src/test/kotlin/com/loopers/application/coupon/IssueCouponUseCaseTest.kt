package com.loopers.application.coupon

import com.loopers.domain.coupon.FakeCouponRepository
import com.loopers.domain.coupon.FakeIssuedCouponRepository
import com.loopers.domain.coupon.model.Coupon
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class IssueCouponUseCaseTest {

    private lateinit var couponRepository: FakeCouponRepository
    private lateinit var issuedCouponRepository: FakeIssuedCouponRepository
    private lateinit var issueCouponUseCase: IssueCouponUseCase

    @BeforeEach
    fun setUp() {
        couponRepository = FakeCouponRepository()
        issuedCouponRepository = FakeIssuedCouponRepository()
        issueCouponUseCase = IssueCouponUseCase(couponRepository, issuedCouponRepository)
    }

    private fun createCoupon(
        totalQuantity: Int? = 100,
        issuedCount: Int = 0,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(7),
        deletedAt: ZonedDateTime? = null,
    ): Coupon {
        return couponRepository.save(
            Coupon(
                name = "테스트 쿠폰",
                type = Coupon.CouponType.FIXED,
                value = 1000L,
                totalQuantity = totalQuantity,
                issuedCount = issuedCount,
                expiredAt = expiredAt,
                deletedAt = deletedAt,
            ),
        )
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("정상 발급 시 IssuedCoupon이 생성되고 Coupon의 issuedCount가 증가한다")
        fun execute_success() {
            // arrange
            val coupon = createCoupon(totalQuantity = 100, issuedCount = 0)
            val userId = 1L

            // act
            val result = issueCouponUseCase.execute(userId, coupon.id.value)

            // assert
            assertThat(result.id).isNotEqualTo(0L)
            assertThat(result.couponId).isEqualTo(coupon.id.value)
            assertThat(result.userId).isEqualTo(userId)
            assertThat(result.status).isEqualTo("AVAILABLE")

            val updatedCoupon = couponRepository.findById(coupon.id)!!
            assertThat(updatedCoupon.issuedCount).isEqualTo(1)
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰 ID이면 NOT_FOUND 예외가 발생한다")
        fun execute_couponNotFound_throwsNotFoundException() {
            // arrange
            val userId = 1L
            val nonExistentCouponId = 999L

            // act
            val exception = assertThrows<CoreException> {
                issueCouponUseCase.execute(userId, nonExistentCouponId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("삭제된 쿠폰이면 NOT_FOUND 예외가 발생한다")
        fun execute_deletedCoupon_throwsNotFoundException() {
            // arrange
            val coupon = createCoupon(deletedAt = ZonedDateTime.now().minusDays(1))
            val userId = 1L

            // act
            val exception = assertThrows<CoreException> {
                issueCouponUseCase.execute(userId, coupon.id.value)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("만료된 쿠폰이면 BAD_REQUEST 예외가 발생한다")
        fun execute_expiredCoupon_throwsBadRequest() {
            // arrange
            val coupon = createCoupon(expiredAt = ZonedDateTime.now().minusDays(1))
            val userId = 1L

            // act
            val exception = assertThrows<CoreException> {
                issueCouponUseCase.execute(userId, coupon.id.value)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("수량이 소진된 쿠폰이면 BAD_REQUEST 예외가 발생한다")
        fun execute_soldOutCoupon_throwsBadRequest() {
            // arrange
            val coupon = createCoupon(totalQuantity = 10, issuedCount = 10)
            val userId = 1L

            // act
            val exception = assertThrows<CoreException> {
                issueCouponUseCase.execute(userId, coupon.id.value)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("이미 발급받은 쿠폰이면 BAD_REQUEST 예외가 발생한다")
        fun execute_alreadyIssuedCoupon_throwsBadRequest() {
            // arrange
            val coupon = createCoupon()
            val userId = 1L
            issueCouponUseCase.execute(userId, coupon.id.value)

            // act
            val exception = assertThrows<CoreException> {
                issueCouponUseCase.execute(userId, coupon.id.value)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
