package com.loopers.application.coupon

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.coupon.FakeCouponRepository
import com.loopers.domain.coupon.FakeIssuedCouponRepository
import com.loopers.domain.coupon.model.Coupon
import com.loopers.domain.coupon.model.IssuedCoupon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class GetMyCouponsUseCaseTest {

    private lateinit var couponRepository: FakeCouponRepository
    private lateinit var issuedCouponRepository: FakeIssuedCouponRepository
    private lateinit var getMyCouponsUseCase: GetMyCouponsUseCase

    @BeforeEach
    fun setUp() {
        couponRepository = FakeCouponRepository()
        issuedCouponRepository = FakeIssuedCouponRepository()
        getMyCouponsUseCase = GetMyCouponsUseCase(couponRepository, issuedCouponRepository)
    }

    private fun createCoupon(name: String = "테스트 쿠폰"): Coupon {
        return couponRepository.save(
            Coupon(
                name = name,
                type = Coupon.CouponType.FIXED,
                value = 1000L,
                totalQuantity = 100,
                expiredAt = ZonedDateTime.now().plusDays(7),
            ),
        )
    }

    private fun createIssuedCoupon(couponId: Long, userId: Long): IssuedCoupon {
        return issuedCouponRepository.save(
            IssuedCoupon(
                refCouponId = couponId,
                refUserId = UserId(userId),
                createdAt = ZonedDateTime.now(),
            ),
        )
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("발급된 쿠폰 목록을 쿠폰 템플릿 정보와 함께 반환한다")
        fun execute_returnsIssuedCouponsWithCouponInfo() {
            // arrange
            val coupon1 = createCoupon("할인쿠폰A")
            val coupon2 = createCoupon("할인쿠폰B")
            val userId = 1L
            createIssuedCoupon(coupon1.id, userId)
            createIssuedCoupon(coupon2.id, userId)

            // act
            val result = getMyCouponsUseCase.execute(userId)

            // assert
            assertThat(result).hasSize(2)
            val names = result.map { it.couponName }
            assertThat(names).containsExactlyInAnyOrder("할인쿠폰A", "할인쿠폰B")
            assertThat(result[0].status).isEqualTo("AVAILABLE")
        }

        @Test
        @DisplayName("발급된 쿠폰이 없으면 빈 리스트를 반환한다")
        fun execute_noCoupons_returnsEmptyList() {
            // arrange
            val userId = 1L

            // act
            val result = getMyCouponsUseCase.execute(userId)

            // assert
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("다른 유저의 쿠폰은 포함되지 않는다")
        fun execute_doesNotIncludeOtherUsersCoupons() {
            // arrange
            val coupon = createCoupon()
            val userId = 1L
            val otherUserId = 2L
            createIssuedCoupon(coupon.id, otherUserId)

            // act
            val result = getMyCouponsUseCase.execute(userId)

            // assert
            assertThat(result).isEmpty()
        }
    }
}
