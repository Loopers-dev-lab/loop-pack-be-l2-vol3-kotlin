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

@DisplayName("CouponService")
class CouponServiceTest {

    private val couponRepository: CouponRepository = mockk()
    private val couponService = CouponService(couponRepository)

    private fun createCoupon(
        name: String = "테스트 쿠폰",
        type: CouponType = CouponType.RATE,
        value: Long = 10L,
        minOrderAmount: Long? = null,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): CouponModel = CouponModel(
        name = name,
        type = type,
        value = value,
        minOrderAmount = minOrderAmount,
        expiredAt = expiredAt,
    )

    @DisplayName("create")
    @Nested
    inner class Create {
        @DisplayName("쿠폰 템플릿을 정상 생성한다")
        @Test
        fun createsCoupon() {
            // arrange
            val coupon = createCoupon()
            every { couponRepository.save(any()) } returns coupon

            // act
            val result = couponService.create(coupon)

            // assert
            assertThat(result.name).isEqualTo("테스트 쿠폰")
            verify(exactly = 1) { couponRepository.save(coupon) }
        }
    }

    @DisplayName("findById")
    @Nested
    inner class FindById {
        @DisplayName("존재하는 ID로 조회하면 CouponModel을 반환한다")
        @Test
        fun returnsCoupon_whenExists() {
            // arrange
            val coupon = createCoupon()
            every { couponRepository.findByIdAndDeletedAtIsNull(1L) } returns coupon

            // act
            val result = couponService.findById(1L)

            // assert
            assertThat(result.name).isEqualTo("테스트 쿠폰")
        }

        @DisplayName("존재하지 않는 ID로 조회하면 CoreException이 발생한다")
        @Test
        fun throwsException_whenNotFound() {
            // arrange
            every { couponRepository.findByIdAndDeletedAtIsNull(999L) } returns null

            // act & assert
            assertThatThrownBy { couponService.findById(999L) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("findAll")
    @Nested
    inner class FindAll {
        @DisplayName("페이징된 쿠폰 목록을 반환한다")
        @Test
        fun returnsPagedCoupons() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val coupons = listOf(createCoupon())
            every { couponRepository.findAllByDeletedAtIsNull(pageable) } returns PageImpl(coupons)

            // act
            val result = couponService.findAll(pageable)

            // assert
            assertThat(result.content).hasSize(1)
        }
    }

    @DisplayName("delete")
    @Nested
    inner class Delete {
        @DisplayName("존재하는 쿠폰을 삭제한다")
        @Test
        fun deletesCoupon_whenExists() {
            // arrange
            val coupon = createCoupon()
            every { couponRepository.findByIdAndDeletedAtIsNull(1L) } returns coupon
            every { couponRepository.save(any()) } returns coupon

            // act
            couponService.delete(1L)

            // assert
            assertThat(coupon.deletedAt).isNotNull()
            verify(exactly = 1) { couponRepository.save(coupon) }
        }
    }
}
