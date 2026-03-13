package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.ZonedDateTime

class CouponRepositoryImplTest {

    private val couponJpaRepository: CouponJpaRepository = mockk()
    private val couponRepositoryImpl = CouponRepositoryImpl(couponJpaRepository)

    private fun createCoupon(): CouponModel = CouponModel(
        name = "테스트 쿠폰",
        type = CouponType.RATE,
        value = 10L,
        expiredAt = ZonedDateTime.now().plusDays(30),
    )

    @DisplayName("쿠폰을 저장할 때,")
    @Nested
    inner class Save {
        @DisplayName("JpaRepository에 위임하여 저장하고 결과를 반환한다.")
        @Test
        fun delegatesToJpaRepository() {
            // arrange
            val coupon = createCoupon()
            every { couponJpaRepository.save(coupon) } returns coupon

            // act
            val result = couponRepositoryImpl.save(coupon)

            // assert
            assertThat(result.name).isEqualTo("테스트 쿠폰")
            verify(exactly = 1) { couponJpaRepository.save(coupon) }
        }
    }

    @DisplayName("쿠폰을 ID로 조회할 때,")
    @Nested
    inner class FindById {
        @DisplayName("존재하면 반환한다.")
        @Test
        fun returnsCoupon_whenExists() {
            // arrange
            val coupon = createCoupon()
            every { couponJpaRepository.findByIdAndDeletedAtIsNull(1L) } returns coupon

            // act
            val result = couponRepositoryImpl.findByIdAndDeletedAtIsNull(1L)

            // assert
            assertThat(result).isNotNull
            verify(exactly = 1) { couponJpaRepository.findByIdAndDeletedAtIsNull(1L) }
        }

        @DisplayName("존재하지 않으면 null을 반환한다.")
        @Test
        fun returnsNull_whenNotExists() {
            // arrange
            every { couponJpaRepository.findByIdAndDeletedAtIsNull(999L) } returns null

            // act
            val result = couponRepositoryImpl.findByIdAndDeletedAtIsNull(999L)

            // assert
            assertThat(result).isNull()
        }
    }

    @DisplayName("쿠폰 목록을 조회할 때,")
    @Nested
    inner class FindAll {
        @DisplayName("페이징된 목록을 반환한다.")
        @Test
        fun returnsPagedCoupons() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val coupons = listOf(createCoupon())
            every { couponJpaRepository.findAllByDeletedAtIsNull(pageable) } returns PageImpl(coupons)

            // act
            val result = couponRepositoryImpl.findAllByDeletedAtIsNull(pageable)

            // assert
            assertThat(result.content).hasSize(1)
        }
    }
}
