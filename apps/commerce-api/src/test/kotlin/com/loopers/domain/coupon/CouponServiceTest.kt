package com.loopers.domain.coupon

import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import com.loopers.support.common.SortOrder
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class CouponServiceTest {

    @Mock
    private lateinit var couponRepository: CouponRepository

    @Mock
    private lateinit var issuedCouponRepository: IssuedCouponRepository

    private lateinit var couponService: CouponService

    @BeforeEach
    fun setUp() {
        couponService = CouponService(couponRepository, issuedCouponRepository)
    }

    @DisplayName("쿠폰을 ID로 조회할 때,")
    @Nested
    inner class FindCouponById {

        @DisplayName("존재하는 쿠폰이면, 쿠폰을 반환한다.")
        @Test
        fun returnsCoupon_whenCouponExists() {
            // arrange
            val coupon = Coupon(
                name = "신규가입 할인",
                discount = Discount(DiscountType.FIXED_AMOUNT, 5000L),
                quantity = CouponQuantity(100, 0),
                expiresAt = ZonedDateTime.now().plusDays(30),
            )
            whenever(couponRepository.findById(1L)).thenReturn(coupon)

            // act
            val result = couponService.findCouponById(1L)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("신규가입 할인") },
                { assertThat(result.discount.type).isEqualTo(DiscountType.FIXED_AMOUNT) },
                { assertThat(result.discount.value).isEqualTo(5000L) },
            )
        }

        @DisplayName("존재하지 않는 쿠폰이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponNotExists() {
            // arrange
            whenever(couponRepository.findById(999L)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                couponService.findCouponById(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("쿠폰 목록을 조회할 때,")
    @Nested
    inner class FindAll {

        @DisplayName("쿠폰이 존재하면, 페이징된 결과를 반환한다.")
        @Test
        fun returnsPagedCoupons_whenCouponsExist() {
            // arrange
            val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)
            val coupon = Coupon(
                name = "신규가입 할인",
                discount = Discount(DiscountType.FIXED_AMOUNT, 5000L),
                quantity = CouponQuantity(100, 0),
                expiresAt = ZonedDateTime.now().plusDays(30),
            )
            val pageResult = PageResult(
                content = listOf(coupon),
                page = 0,
                size = 20,
                totalElements = 1L,
                totalPages = 1,
            )
            whenever(couponRepository.findAll(pageQuery)).thenReturn(pageResult)

            // act
            val result = couponService.findAll(pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content[0].name).isEqualTo("신규가입 할인") },
                { assertThat(result.totalElements).isEqualTo(1L) },
            )
        }

        @DisplayName("쿠폰이 없으면, 빈 페이지를 반환한다.")
        @Test
        fun returnsEmptyPage_whenNoCouponsExist() {
            // arrange
            val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)
            val emptyPage = PageResult<Coupon>(
                content = emptyList(),
                page = 0,
                size = 20,
                totalElements = 0L,
                totalPages = 0,
            )
            whenever(couponRepository.findAll(pageQuery)).thenReturn(emptyPage)

            // act
            val result = couponService.findAll(pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).isEmpty() },
                { assertThat(result.totalElements).isEqualTo(0L) },
            )
        }
    }

    @DisplayName("쿠폰을 삭제할 때,")
    @Nested
    inner class DeleteCoupon {

        @DisplayName("존재하는 쿠폰이면, soft delete가 수행된다.")
        @Test
        fun deletesCoupon_whenCouponExists() {
            // arrange
            val coupon = Coupon(
                name = "삭제할 쿠폰",
                discount = Discount(DiscountType.FIXED_AMOUNT, 5000L),
                quantity = CouponQuantity(100, 0),
                expiresAt = ZonedDateTime.now().plusDays(30),
            )
            whenever(couponRepository.findById(1L)).thenReturn(coupon)

            // act
            couponService.delete(1L)

            // assert
            verify(couponRepository).save(coupon)
            assertThat(coupon.deletedAt).isNotNull()
        }

        @DisplayName("존재하지 않는 쿠폰이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponNotExists() {
            // arrange
            whenever(couponRepository.findById(999L)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                couponService.delete(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
