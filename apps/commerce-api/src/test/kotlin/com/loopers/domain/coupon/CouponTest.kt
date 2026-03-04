package com.loopers.domain.coupon

import com.loopers.domain.product.Money
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.ZonedDateTime

class CouponTest {

    @Test
    fun `create로 생성한 Coupon의 persistenceId는 null이어야 한다`() {
        val coupon = createCoupon()

        assertThat(coupon.persistenceId).isNull()
    }

    @Test
    fun `create로 생성한 Coupon의 issuedCount는 0이어야 한다`() {
        val coupon = createCoupon()

        assertThat(coupon.issuedCount).isEqualTo(0)
    }

    @Test
    fun `create로 생성한 Coupon의 deletedAt은 null이어야 한다`() {
        val coupon = createCoupon()

        assertThat(coupon.deletedAt).isNull()
    }

    @Test
    fun `reconstitute로 생성한 Coupon은 persistenceId를 가져야 한다`() {
        val coupon = Coupon.reconstitute(
            persistenceId = 1L,
            name = CouponName(COUPON_NAME),
            discountType = DiscountType.FIXED,
            discountValue = DISCOUNT_VALUE,
            minOrderAmount = Money(MIN_ORDER_AMOUNT),
            maxIssueCount = MAX_ISSUE_COUNT,
            issuedCount = 0,
            expiredAt = FUTURE_DATE,
            deletedAt = null,
        )

        assertThat(coupon.persistenceId).isEqualTo(1L)
    }

    @Test
    fun `만료되지 않은 쿠폰의 경우 isExpired가 false를 반환해야 한다`() {
        val coupon = createCoupon()

        assertThat(coupon.isExpired()).isFalse()
    }

    @Test
    fun `만료된 쿠폰의 경우 isExpired가 true를 반환해야 한다`() {
        val coupon = createCoupon(expiredAt = PAST_DATE)

        assertThat(coupon.isExpired()).isTrue()
    }

    @Test
    fun `발급 가능한 쿠폰의 경우 canIssue가 true를 반환해야 한다`() {
        val coupon = createCoupon()

        assertThat(coupon.canIssue()).isTrue()
    }

    @Test
    fun `만료된 쿠폰의 경우 canIssue가 false를 반환해야 한다`() {
        val coupon = createCoupon(expiredAt = PAST_DATE)

        assertThat(coupon.canIssue()).isFalse()
    }

    @Test
    fun `발급 수량이 가득 찬 경우 canIssue가 false를 반환해야 한다`() {
        val coupon = Coupon.reconstitute(
            persistenceId = 1L,
            name = CouponName(COUPON_NAME),
            discountType = DiscountType.FIXED,
            discountValue = DISCOUNT_VALUE,
            minOrderAmount = Money(MIN_ORDER_AMOUNT),
            maxIssueCount = 10,
            issuedCount = 10,
            expiredAt = FUTURE_DATE,
            deletedAt = null,
        )

        assertThat(coupon.canIssue()).isFalse()
    }

    @Test
    fun `maxIssueCount가 null이면 수량 제한 없이 canIssue가 true를 반환해야 한다`() {
        val coupon = createCoupon(maxIssueCount = null)

        assertThat(coupon.canIssue()).isTrue()
    }

    @Test
    fun `발급 가능한 쿠폰의 경우 assertIssuable이 성공해야 한다`() {
        val coupon = createCoupon()

        assertDoesNotThrow { coupon.assertIssuable() }
    }

    @Test
    fun `만료된 쿠폰의 경우 assertIssuable이 CouponException을 던져야 한다`() {
        val coupon = createCoupon(expiredAt = PAST_DATE)

        assertThatThrownBy { coupon.assertIssuable() }
            .isInstanceOf(CouponException::class.java)
    }

    @Test
    fun `발급 수량 초과의 경우 assertIssuable이 CouponException을 던져야 한다`() {
        val coupon = Coupon.reconstitute(
            persistenceId = 1L,
            name = CouponName(COUPON_NAME),
            discountType = DiscountType.FIXED,
            discountValue = DISCOUNT_VALUE,
            minOrderAmount = Money(MIN_ORDER_AMOUNT),
            maxIssueCount = 10,
            issuedCount = 10,
            expiredAt = FUTURE_DATE,
            deletedAt = null,
        )

        assertThatThrownBy { coupon.assertIssuable() }
            .isInstanceOf(CouponException::class.java)
    }

    @Test
    fun `update 호출시 새 Coupon 인스턴스를 반환해야 한다`() {
        val coupon = Coupon.reconstitute(
            persistenceId = 1L,
            name = CouponName(COUPON_NAME),
            discountType = DiscountType.FIXED,
            discountValue = DISCOUNT_VALUE,
            minOrderAmount = Money(MIN_ORDER_AMOUNT),
            maxIssueCount = MAX_ISSUE_COUNT,
            issuedCount = 0,
            expiredAt = FUTURE_DATE,
            deletedAt = null,
        )

        val updated = coupon.update(
            name = CouponName("수정된 쿠폰"),
            discountType = DiscountType.RATE,
            discountValue = 15,
            minOrderAmount = Money(20000),
            maxIssueCount = 200,
            expiredAt = FUTURE_DATE,
        )

        assertThat(updated).isNotSameAs(coupon)
        assertThat(updated.name.value).isEqualTo("수정된 쿠폰")
        assertThat(updated.discountType).isEqualTo(DiscountType.RATE)
    }

    @Test
    fun `delete 호출시 deletedAt이 설정되어야 한다`() {
        val coupon = Coupon.reconstitute(
            persistenceId = 1L,
            name = CouponName(COUPON_NAME),
            discountType = DiscountType.FIXED,
            discountValue = DISCOUNT_VALUE,
            minOrderAmount = Money(MIN_ORDER_AMOUNT),
            maxIssueCount = MAX_ISSUE_COUNT,
            issuedCount = 0,
            expiredAt = FUTURE_DATE,
            deletedAt = null,
        )

        val deleted = coupon.delete()

        assertThat(deleted.isDeleted()).isTrue()
        assertThat(deleted.deletedAt).isNotNull()
    }

    @Test
    fun `RATE 할인의 경우 discountValue가 1~100 범위여야 한다`() {
        assertThatThrownBy {
            Coupon.create(
                name = CouponName(COUPON_NAME),
                discountType = DiscountType.RATE,
                discountValue = 0,
                minOrderAmount = Money(MIN_ORDER_AMOUNT),
                maxIssueCount = MAX_ISSUE_COUNT,
                expiredAt = FUTURE_DATE,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)

        assertThatThrownBy {
            Coupon.create(
                name = CouponName(COUPON_NAME),
                discountType = DiscountType.RATE,
                discountValue = 101,
                minOrderAmount = Money(MIN_ORDER_AMOUNT),
                maxIssueCount = MAX_ISSUE_COUNT,
                expiredAt = FUTURE_DATE,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `FIXED 할인의 경우 discountValue가 양수여야 한다`() {
        assertThatThrownBy {
            Coupon.create(
                name = CouponName(COUPON_NAME),
                discountType = DiscountType.FIXED,
                discountValue = 0,
                minOrderAmount = Money(MIN_ORDER_AMOUNT),
                maxIssueCount = MAX_ISSUE_COUNT,
                expiredAt = FUTURE_DATE,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun createCoupon(
        expiredAt: ZonedDateTime = FUTURE_DATE,
        maxIssueCount: Int? = MAX_ISSUE_COUNT,
    ): Coupon = Coupon.create(
        name = CouponName(COUPON_NAME),
        discountType = DiscountType.FIXED,
        discountValue = DISCOUNT_VALUE,
        minOrderAmount = Money(MIN_ORDER_AMOUNT),
        maxIssueCount = maxIssueCount,
        expiredAt = expiredAt,
    )

    companion object {
        private const val COUPON_NAME = "신규 가입 축하 쿠폰"
        private const val DISCOUNT_VALUE = 5000L
        private const val MIN_ORDER_AMOUNT = 10000L
        private const val MAX_ISSUE_COUNT = 100
        private val FUTURE_DATE: ZonedDateTime = ZonedDateTime.now().plusDays(30)
        private val PAST_DATE: ZonedDateTime = ZonedDateTime.now().minusDays(1)
    }
}
