package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponQuantity
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.Discount
import com.loopers.domain.coupon.DiscountType
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZonedDateTime

@SpringBootTest
class AdminCouponFacadeIntegrationTest @Autowired constructor(
    private val adminCouponFacade: AdminCouponFacade,
    private val couponRepository: CouponRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createCoupon(
        name: String = "신규가입 할인",
        discount: Discount = Discount(DiscountType.FIXED_AMOUNT, 5000L),
        totalQuantity: Int = 100,
        expiresAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): Coupon {
        return couponRepository.save(
            Coupon(
                name = name,
                discount = discount,
                quantity = CouponQuantity(totalQuantity, 0),
                expiresAt = expiresAt,
            ),
        )
    }

    @DisplayName("쿠폰 삭제할 때,")
    @Nested
    inner class DeleteCoupon {

        @DisplayName("DB에 저장된 쿠폰을 삭제하면, soft delete 되어 조회할 수 없다.")
        @Test
        fun softDeletesCoupon_whenCouponExistsInDb() {
            // arrange
            val saved = createCoupon(name = "삭제할 쿠폰")

            // act
            adminCouponFacade.deleteCoupon(saved.id)

            // assert
            val exception = assertThrows<CoreException> {
                adminCouponFacade.getCoupon(saved.id)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("존재하지 않는 쿠폰 ID로 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponNotExistsInDb() {
            // act
            val exception = assertThrows<CoreException> {
                adminCouponFacade.deleteCoupon(999999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("이미 삭제된 쿠폰을 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponAlreadyDeleted() {
            // arrange
            val saved = createCoupon(name = "삭제될 쿠폰")
            adminCouponFacade.deleteCoupon(saved.id)

            // act
            val exception = assertThrows<CoreException> {
                adminCouponFacade.deleteCoupon(saved.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
