package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.ZonedDateTime

/**
 * CouponService 통합 테스트
 * - 실제 DB(TestContainers)와 연동하여 Service → Repository 레이어 통합 테스트
 * - @Transactional 경계가 Service에 있으므로 Service를 통해 테스트
 */
@SpringBootTest
class CouponServiceIntegrationTest @Autowired constructor(
    private val couponService: CouponService,
    private val couponJpaRepository: CouponJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createTestCoupon(
        name: String = "5000원 할인 쿠폰",
        type: CouponType = CouponType.FIXED,
        value: BigDecimal = BigDecimal("5000"),
        minOrderAmount: BigDecimal? = BigDecimal("10000"),
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): Coupon {
        return couponJpaRepository.save(
            Coupon(
                name = name,
                type = type,
                value = value,
                minOrderAmount = minOrderAmount,
                expiredAt = expiredAt,
            ),
        )
    }

    @DisplayName("쿠폰을 등록할 때,")
    @Nested
    inner class CreateCoupon {

        @DisplayName("정상적인 정보가 주어지면, 쿠폰이 DB에 저장된다.")
        @Test
        fun savesCouponToDatabase_whenValidInfoProvided() {
            // arrange
            val criteria = CreateCouponCriteria(
                name = "5000원 할인 쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // act
            val result = couponService.createCoupon(criteria)

            // assert
            val saved = couponJpaRepository.findByIdAndDeletedAtIsNull(result.id)!!
            assertAll(
                { assertThat(saved.name).isEqualTo("5000원 할인 쿠폰") },
                { assertThat(saved.type).isEqualTo(CouponType.FIXED) },
                { assertThat(saved.value).isEqualByComparingTo(BigDecimal("5000")) },
                { assertThat(saved.minOrderAmount).isEqualByComparingTo(BigDecimal("10000")) },
            )
        }
    }

    @DisplayName("쿠폰을 조회할 때,")
    @Nested
    inner class GetCoupon {

        @DisplayName("존재하는 쿠폰 ID로 조회하면, 쿠폰 정보가 반환된다.")
        @Test
        fun returnsCoupon_whenCouponExists() {
            // arrange
            val saved = createTestCoupon()

            // act
            val result = couponService.getCouponInfo(saved.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(saved.id) },
                { assertThat(result.name).isEqualTo("5000원 할인 쿠폰") },
                { assertThat(result.type).isEqualTo(CouponType.FIXED) },
            )
        }

        @DisplayName("soft delete된 쿠폰은 조회되지 않는다.")
        @Test
        fun throwsNotFound_whenCouponIsSoftDeleted() {
            // arrange
            val saved = createTestCoupon()
            saved.delete()
            couponJpaRepository.save(saved)

            // act & assert
            val exception = assertThrows<CoreException> {
                couponService.getCouponInfo(saved.id)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("쿠폰 목록을 조회할 때,")
    @Nested
    inner class GetAllCoupons {

        @DisplayName("쿠폰이 존재하면, 페이징된 목록이 반환된다.")
        @Test
        fun returnsCouponList_whenCouponsExist() {
            // arrange
            createTestCoupon(name = "쿠폰 A")
            createTestCoupon(name = "쿠폰 B")

            // act
            val result = couponService.getAllCoupons(PageRequest.of(0, 20))

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.totalElements).isEqualTo(2L) },
            )
        }

        @DisplayName("soft delete된 쿠폰은 목록에 포함되지 않는다.")
        @Test
        fun excludesSoftDeletedCoupons() {
            // arrange
            createTestCoupon(name = "쿠폰 A")
            val deleted = createTestCoupon(name = "쿠폰 B")
            deleted.delete()
            couponJpaRepository.save(deleted)

            // act
            val result = couponService.getAllCoupons(PageRequest.of(0, 20))

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content[0].name).isEqualTo("쿠폰 A") },
            )
        }
    }

    @DisplayName("쿠폰을 수정할 때,")
    @Nested
    inner class UpdateCoupon {

        @DisplayName("정상적인 정보가 주어지면, 쿠폰이 DB에서 수정된다.")
        @Test
        fun updatesCouponInDatabase_whenValidInfoProvided() {
            // arrange
            val saved = createTestCoupon()
            val criteria = UpdateCouponCriteria(
                name = "수정된 쿠폰",
                value = BigDecimal("3000"),
                minOrderAmount = BigDecimal("5000"),
                expiredAt = ZonedDateTime.now().plusDays(60),
            )

            // act
            couponService.updateCoupon(saved.id, criteria)

            // assert
            val updated = couponJpaRepository.findByIdAndDeletedAtIsNull(saved.id)!!
            assertAll(
                { assertThat(updated.name).isEqualTo("수정된 쿠폰") },
                { assertThat(updated.value).isEqualByComparingTo(BigDecimal("3000")) },
                { assertThat(updated.minOrderAmount).isEqualByComparingTo(BigDecimal("5000")) },
            )
        }

        @DisplayName("존재하지 않는 쿠폰을 수정하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponNotExists() {
            // arrange
            val criteria = UpdateCouponCriteria(
                name = "수정된 쿠폰",
                value = BigDecimal("3000"),
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(60),
            )

            // act & assert
            val exception = assertThrows<CoreException> {
                couponService.updateCoupon(999L, criteria)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("쿠폰을 삭제할 때,")
    @Nested
    inner class DeleteCoupon {

        @DisplayName("존재하는 쿠폰을 삭제하면, soft delete 되어 조회되지 않는다.")
        @Test
        fun softDeletesCoupon_whenCouponExists() {
            // arrange
            val saved = createTestCoupon()

            // act
            couponService.deleteCoupon(saved.id)

            // assert
            val deleted = couponJpaRepository.findById(saved.id).get()
            assertAll(
                { assertThat(deleted.isDeleted()).isTrue() },
                { assertThat(couponJpaRepository.findByIdAndDeletedAtIsNull(saved.id)).isNull() },
            )
        }

        @DisplayName("존재하지 않는 쿠폰을 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponNotExists() {
            // act & assert
            val exception = assertThrows<CoreException> {
                couponService.deleteCoupon(999L)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("쿠폰을 발급할 때,")
    @Nested
    inner class IssueCoupon {

        @DisplayName("정상적으로 발급되면, AVAILABLE 상태로 저장된다.")
        @Test
        fun issuesCouponWithAvailableStatus_whenValidRequest() {
            // arrange
            val coupon = createTestCoupon()
            val userId = 1L

            // act
            val result = couponService.issueCoupon(coupon.id, userId)

            // assert
            val saved = issuedCouponJpaRepository.findById(result.id).get()
            assertAll(
                { assertThat(saved.couponId).isEqualTo(coupon.id) },
                { assertThat(saved.userId).isEqualTo(userId) },
                { assertThat(saved.isUsable()).isTrue() },
            )
        }

        @DisplayName("만료된 쿠폰은 발급할 수 없다.")
        @Test
        fun throwsBadRequest_whenCouponExpired() {
            // arrange
            val coupon = createTestCoupon(expiredAt = ZonedDateTime.now().minusDays(1))
            val userId = 1L

            // act & assert
            val exception = assertThrows<CoreException> {
                couponService.issueCoupon(coupon.id, userId)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이미 발급받은 쿠폰은 중복 발급할 수 없다.")
        @Test
        fun throwsConflict_whenAlreadyIssued() {
            // arrange
            val coupon = createTestCoupon()
            val userId = 1L
            issuedCouponJpaRepository.save(IssuedCoupon(couponId = coupon.id, userId = userId))

            // act & assert
            val exception = assertThrows<CoreException> {
                couponService.issueCoupon(coupon.id, userId)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }

    @DisplayName("내 쿠폰 목록을 조회할 때,")
    @Nested
    inner class GetMyCoupons {

        @DisplayName("사용자별 발급된 쿠폰 목록이 반환된다.")
        @Test
        fun returnsCoupons_whenUserHasCoupons() {
            // arrange
            val coupon1 = createTestCoupon(name = "쿠폰 A")
            val coupon2 = createTestCoupon(name = "쿠폰 B")
            val userId = 1L
            issuedCouponJpaRepository.save(IssuedCoupon(couponId = coupon1.id, userId = userId))
            issuedCouponJpaRepository.save(IssuedCoupon(couponId = coupon2.id, userId = userId))

            // act
            val result = couponService.getMyCoupons(userId)

            // assert
            assertThat(result).hasSize(2)
        }

        @DisplayName("다른 사용자의 쿠폰은 조회되지 않는다.")
        @Test
        fun excludesOtherUsersCoupons() {
            // arrange
            val coupon = createTestCoupon()
            issuedCouponJpaRepository.save(IssuedCoupon(couponId = coupon.id, userId = 1L))
            issuedCouponJpaRepository.save(IssuedCoupon(couponId = coupon.id, userId = 2L))

            // act
            val result = couponService.getMyCoupons(1L)

            // assert
            assertThat(result).hasSize(1)
        }
    }

    @DisplayName("쿠폰 발급 내역을 조회할 때,")
    @Nested
    inner class GetIssuedCoupons {

        @DisplayName("쿠폰별 발급 내역을 페이징 조회한다.")
        @Test
        fun returnsIssuedCouponPage_whenCalled() {
            // arrange
            val coupon = createTestCoupon()
            issuedCouponJpaRepository.save(IssuedCoupon(couponId = coupon.id, userId = 1L))
            issuedCouponJpaRepository.save(IssuedCoupon(couponId = coupon.id, userId = 2L))

            // act
            val result = couponService.getIssuedCoupons(coupon.id, PageRequest.of(0, 20))

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.totalElements).isEqualTo(2L) },
            )
        }
    }
}
