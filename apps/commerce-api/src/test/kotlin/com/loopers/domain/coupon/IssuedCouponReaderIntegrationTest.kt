package com.loopers.domain.coupon

import com.loopers.infrastructure.coupon.CouponEntity
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponEntity
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZonedDateTime

@SpringBootTest
class IssuedCouponReaderIntegrationTest @Autowired constructor(
    private val issuedCouponReader: IssuedCouponReader,
    private val couponJpaRepository: CouponJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    inner class GetById {
        @Test
        fun `발급된_쿠폰을_조회할_수_있다`() {
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

            // act
            val issuedCoupon = issuedCouponReader.getById(issuedEntity.id!!)

            // assert
            assertAll(
                { assertThat(issuedCoupon.id).isEqualTo(issuedEntity.id) },
                { assertThat(issuedCoupon.couponId).isEqualTo(couponEntity.id) },
                { assertThat(issuedCoupon.memberId).isEqualTo(1L) },
                { assertThat(issuedCoupon.status).isEqualTo(CouponStatus.AVAILABLE) },
            )
        }

        @Test
        fun `존재하지_않는_발급_쿠폰을_조회하면_예외가_발생한다`() {
            val result = assertThrows<CoreException> {
                issuedCouponReader.getById(999L)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.COUPON_NOT_FOUND)
        }
    }

    @Nested
    inner class GetAllByMemberId {
        @Test
        fun `회원의_발급_쿠폰_목록을_조회할_수_있다`() {
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
            issuedCouponJpaRepository.save(
                IssuedCouponEntity(
                    couponId = couponEntity.id!!,
                    memberId = 1L,
                    status = CouponStatus.AVAILABLE.name,
                    issuedAt = ZonedDateTime.now(),
                ),
            )

            // act
            val issuedCoupons = issuedCouponReader.getAllByMemberId(1L)

            // assert
            assertThat(issuedCoupons).hasSize(1)
        }
    }
}
