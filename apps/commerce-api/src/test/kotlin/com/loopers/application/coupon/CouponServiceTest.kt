package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.domain.coupon.IssuedCouponStatus
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class CouponServiceTest {

    @Mock
    private lateinit var couponRepository: CouponRepository

    @Mock
    private lateinit var issuedCouponRepository: IssuedCouponRepository

    @InjectMocks
    private lateinit var couponService: CouponService

    private fun createCoupon(
        id: Long = 1L,
        name: String = "5000원 할인 쿠폰",
        type: CouponType = CouponType.FIXED,
        value: BigDecimal = BigDecimal("5000"),
        minOrderAmount: BigDecimal? = BigDecimal("10000"),
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): Coupon {
        val coupon = Coupon(
            name = name,
            type = type,
            value = value,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
        )
        ReflectionTestUtils.setField(coupon, "id", id)
        ReflectionTestUtils.setField(coupon, "createdAt", ZonedDateTime.now())
        ReflectionTestUtils.setField(coupon, "updatedAt", ZonedDateTime.now())
        return coupon
    }

    private fun createIssuedCoupon(
        id: Long = 1L,
        couponId: Long = 1L,
        userId: Long = 1L,
        status: IssuedCouponStatus = IssuedCouponStatus.AVAILABLE,
    ): IssuedCoupon {
        val issuedCoupon = IssuedCoupon(
            couponId = couponId,
            userId = userId,
            status = status,
        )
        ReflectionTestUtils.setField(issuedCoupon, "id", id)
        ReflectionTestUtils.setField(issuedCoupon, "createdAt", ZonedDateTime.now())
        ReflectionTestUtils.setField(issuedCoupon, "updatedAt", ZonedDateTime.now())
        return issuedCoupon
    }

    @DisplayName("쿠폰 템플릿을 등록할 때,")
    @Nested
    inner class CreateCoupon {

        @DisplayName("정상적으로 등록된다.")
        @Test
        fun createsCoupon_whenValidCriteriaProvided() {
            // arrange
            val criteria = CreateCouponCriteria(
                name = "5000원 할인 쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )
            val coupon = createCoupon()
            whenever(couponRepository.save(any())).thenReturn(coupon)

            // act
            val result = couponService.createCoupon(criteria)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("5000원 할인 쿠폰") },
                { assertThat(result.type).isEqualTo(CouponType.FIXED) },
                { assertThat(result.value).isEqualByComparingTo(BigDecimal("5000")) },
            )
        }
    }

    @DisplayName("쿠폰 템플릿을 조회할 때,")
    @Nested
    inner class GetCoupon {

        @DisplayName("존재하는 쿠폰이면, 정상 반환한다.")
        @Test
        fun returnsCoupon_whenCouponExists() {
            // arrange
            val coupon = createCoupon()
            whenever(couponRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(coupon)

            // act
            val result = couponService.getCouponInfo(1L)

            // assert
            assertThat(result.id).isEqualTo(1L)
        }

        @DisplayName("존재하지 않는 쿠폰이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponDoesNotExist() {
            // arrange
            whenever(couponRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                couponService.getCouponInfo(1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("쿠폰 목록을 페이징 조회한다.")
        @Test
        fun returnsCouponPage_whenCalled() {
            // arrange
            val pageable = PageRequest.of(0, 10)
            val coupons = listOf(createCoupon())
            whenever(couponRepository.findAll(pageable)).thenReturn(PageImpl(coupons, pageable, 1))

            // act
            val result = couponService.getAllCoupons(pageable)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.totalElements).isEqualTo(1) },
            )
        }
    }

    @DisplayName("쿠폰 템플릿을 수정할 때,")
    @Nested
    inner class UpdateCoupon {

        @DisplayName("정상적으로 수정된다.")
        @Test
        fun updatesCoupon_whenValidCriteriaProvided() {
            // arrange
            val coupon = createCoupon()
            val criteria = UpdateCouponCriteria(
                name = "수정된 쿠폰",
                value = BigDecimal("3000"),
                minOrderAmount = BigDecimal("5000"),
                expiredAt = ZonedDateTime.now().plusDays(60),
            )
            whenever(couponRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(coupon)
            whenever(couponRepository.save(any())).thenReturn(coupon)

            // act
            val result = couponService.updateCoupon(1L, criteria)

            // assert
            assertThat(result.name).isEqualTo("수정된 쿠폰")
        }

        @DisplayName("존재하지 않는 쿠폰이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponDoesNotExist() {
            // arrange
            val criteria = UpdateCouponCriteria(
                name = "수정된 쿠폰",
                value = BigDecimal("3000"),
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(60),
            )
            whenever(couponRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                couponService.updateCoupon(1L, criteria)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("쿠폰 템플릿을 삭제할 때,")
    @Nested
    inner class DeleteCoupon {

        @DisplayName("정상적으로 Soft Delete 된다.")
        @Test
        fun deletesCoupon_whenCouponExists() {
            // arrange
            val coupon = createCoupon()
            whenever(couponRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(coupon)
            whenever(couponRepository.save(any())).thenReturn(coupon)

            // act
            couponService.deleteCoupon(1L)

            // assert
            verify(couponRepository).save(any())
        }

        @DisplayName("존재하지 않는 쿠폰이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponDoesNotExist() {
            // arrange
            whenever(couponRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                couponService.deleteCoupon(1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("쿠폰을 발급할 때,")
    @Nested
    inner class IssueCoupon {

        @DisplayName("정상적으로 발급되면, AVAILABLE 상태가 된다.")
        @Test
        fun issuesCoupon_whenValidRequest() {
            // arrange
            val coupon = createCoupon()
            whenever(couponRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(coupon)
            whenever(issuedCouponRepository.existsByCouponIdAndUserId(1L, 1L)).thenReturn(false)
            val issuedCoupon = createIssuedCoupon()
            whenever(issuedCouponRepository.save(any())).thenReturn(issuedCoupon)

            // act
            val result = couponService.issueCoupon(1L, 1L)

            // assert
            assertThat(result.status).isEqualTo(IssuedCouponStatus.AVAILABLE)
        }

        @DisplayName("이미 발급받은 쿠폰이면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenAlreadyIssued() {
            // arrange
            val coupon = createCoupon()
            whenever(couponRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(coupon)
            whenever(issuedCouponRepository.existsByCouponIdAndUserId(1L, 1L)).thenReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                couponService.issueCoupon(1L, 1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("만료된 쿠폰이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenCouponExpired() {
            // arrange
            val coupon = createCoupon(expiredAt = ZonedDateTime.now().minusDays(1))
            whenever(couponRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(coupon)

            // act
            val exception = assertThrows<CoreException> {
                couponService.issueCoupon(1L, 1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("삭제된 쿠폰이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponDeleted() {
            // arrange
            whenever(couponRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                couponService.issueCoupon(1L, 1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("내 쿠폰 목록을 조회할 때,")
    @Nested
    inner class GetMyCoupons {

        @DisplayName("정상적으로 조회된다.")
        @Test
        fun returnsCoupons_whenUserHasCoupons() {
            // arrange
            val issuedCoupons = listOf(
                createIssuedCoupon(id = 1L, status = IssuedCouponStatus.AVAILABLE),
                createIssuedCoupon(id = 2L, status = IssuedCouponStatus.USED),
            )
            whenever(issuedCouponRepository.findAllByUserId(1L)).thenReturn(issuedCoupons)

            // act
            val result = couponService.getMyCoupons(1L)

            // assert
            assertThat(result).hasSize(2)
        }
    }

    @DisplayName("쿠폰 발급 내역을 조회할 때,")
    @Nested
    inner class GetIssuedCoupons {

        @DisplayName("쿠폰별 발급 내역을 페이징 조회한다.")
        @Test
        fun returnsIssuedCouponPage_whenCalled() {
            // arrange
            val pageable = PageRequest.of(0, 10)
            val issuedCoupons = listOf(createIssuedCoupon())
            whenever(issuedCouponRepository.findAllByCouponId(1L, pageable))
                .thenReturn(PageImpl(issuedCoupons, pageable, 1))

            // act
            val result = couponService.getIssuedCoupons(1L, pageable)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.totalElements).isEqualTo(1) },
            )
        }
    }

    @DisplayName("주문에 쿠폰을 적용할 때,")
    @Nested
    inner class UseIssuedCoupon {

        @DisplayName("비관적 락으로 조회 후 사용 처리한다.")
        @Test
        fun usesIssuedCoupon_whenValid() {
            // arrange
            val issuedCoupon = createIssuedCoupon()
            whenever(issuedCouponRepository.findByIdWithLock(1L)).thenReturn(issuedCoupon)

            // act
            val result = couponService.getIssuedCouponWithLock(1L)

            // assert
            assertThat(result.id).isEqualTo(1L)
        }

        @DisplayName("존재하지 않는 쿠폰이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenIssuedCouponDoesNotExist() {
            // arrange
            whenever(issuedCouponRepository.findByIdWithLock(1L)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                couponService.getIssuedCouponWithLock(1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
