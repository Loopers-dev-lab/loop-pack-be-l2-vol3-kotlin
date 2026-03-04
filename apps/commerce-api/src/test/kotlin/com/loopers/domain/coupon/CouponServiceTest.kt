package com.loopers.domain.coupon

import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import com.loopers.support.common.SortOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
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
}
