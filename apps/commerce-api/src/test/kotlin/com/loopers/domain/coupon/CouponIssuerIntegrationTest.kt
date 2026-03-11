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
class CouponIssuerIntegrationTest @Autowired constructor(
    private val couponIssuer: CouponIssuer,
    private val couponJpaRepository: CouponJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    inner class Issue {
        @Test
        fun `쿠폰을_발급받을_수_있다`() {
            // arrange
            val couponEntity = createAndSaveCouponEntity()

            // act
            val issuedCoupon = couponIssuer.issue(couponEntity.id!!, 1L)

            // assert
            assertAll(
                { assertThat(issuedCoupon.id).isNotNull() },
                { assertThat(issuedCoupon.couponId).isEqualTo(couponEntity.id) },
                { assertThat(issuedCoupon.memberId).isEqualTo(1L) },
                { assertThat(issuedCoupon.status).isEqualTo(CouponStatus.AVAILABLE) },
            )
        }

        @Test
        fun `이미_발급받은_쿠폰을_중복_발급하면_예외가_발생한다`() {
            // arrange
            val couponEntity = createAndSaveCouponEntity()
            issuedCouponJpaRepository.save(
                IssuedCouponEntity(
                    couponId = couponEntity.id!!,
                    memberId = 1L,
                    status = CouponStatus.AVAILABLE.name,
                    issuedAt = ZonedDateTime.now(),
                ),
            )

            // act
            val result = assertThrows<CoreException> {
                couponIssuer.issue(couponEntity.id!!, 1L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.DUPLICATE_COUPON_ISSUE)
        }

        @Test
        fun `만료된_쿠폰은_발급할_수_없다`() {
            // arrange
            val couponEntity = createAndSaveCouponEntity(
                expiredAt = ZonedDateTime.now().minusDays(1),
            )

            // act
            val result = assertThrows<CoreException> {
                couponIssuer.issue(couponEntity.id!!, 1L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.COUPON_EXPIRED)
        }
    }

    private fun createAndSaveCouponEntity(
        name: String = "테스트쿠폰",
        type: String = CouponType.FIXED.name,
        discountValue: Long = 3000L,
        minOrderAmount: Long? = null,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): CouponEntity {
        return couponJpaRepository.save(
            CouponEntity(
                name = name,
                type = type,
                discountValue = discountValue,
                minOrderAmount = minOrderAmount,
                expiredAt = expiredAt,
            ),
        )
    }
}
