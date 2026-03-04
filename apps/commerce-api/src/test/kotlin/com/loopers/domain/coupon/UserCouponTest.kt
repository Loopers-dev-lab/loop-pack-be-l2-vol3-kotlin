package com.loopers.domain.coupon

import com.loopers.domain.product.Money
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.ZonedDateTime

class UserCouponTest {

    @Test
    fun `issue로 생성한 UserCoupon의 persistenceId는 null이어야 한다`() {
        val userCoupon = createUserCoupon()

        assertThat(userCoupon.persistenceId).isNull()
    }

    @Test
    fun `issue로 생성한 UserCoupon의 상태는 AVAILABLE이어야 한다`() {
        val userCoupon = createUserCoupon()

        assertThat(userCoupon.status).isEqualTo(CouponStatus.AVAILABLE)
    }

    @Test
    fun `issue로 생성한 UserCoupon은 쿠폰 정보를 스냅샷해야 한다`() {
        val coupon = createCoupon()
        val userCoupon = UserCoupon.issue(coupon, USER_ID)

        assertThat(userCoupon.discountType).isEqualTo(coupon.discountType)
        assertThat(userCoupon.discountValue).isEqualTo(coupon.discountValue)
        assertThat(userCoupon.minOrderAmount).isEqualTo(coupon.minOrderAmount)
        assertThat(userCoupon.expiredAt).isEqualTo(coupon.expiredAt)
        assertThat(userCoupon.refCouponId).isEqualTo(coupon.persistenceId)
    }

    @Test
    fun `AVAILABLE 상태의 경우 use가 USED 상태를 반환해야 한다`() {
        val userCoupon = createUserCoupon()

        val used = userCoupon.use()

        assertThat(used.status).isEqualTo(CouponStatus.USED)
        assertThat(used.usedAt).isNotNull()
    }

    @Test
    fun `use 호출시 새 UserCoupon 인스턴스를 반환해야 한다`() {
        val userCoupon = createUserCoupon()

        val used = userCoupon.use()

        assertThat(used).isNotSameAs(userCoupon)
    }

    @Test
    fun `USED 상태의 경우 use가 CouponException을 던져야 한다`() {
        val userCoupon = createUserCoupon().use()

        assertThatThrownBy { userCoupon.use() }
            .isInstanceOf(CouponException::class.java)
    }

    @Test
    fun `USED 상태의 경우 restore가 AVAILABLE 상태를 반환해야 한다`() {
        val userCoupon = createUserCoupon().use()

        val restored = userCoupon.restore()

        assertThat(restored.status).isEqualTo(CouponStatus.AVAILABLE)
        assertThat(restored.usedAt).isNull()
    }

    @Test
    fun `AVAILABLE 상태의 경우 restore가 CouponException을 던져야 한다`() {
        val userCoupon = createUserCoupon()

        assertThatThrownBy { userCoupon.restore() }
            .isInstanceOf(CouponException::class.java)
    }

    @Test
    fun `본인 쿠폰이고 사용 가능한 경우 assertUsableBy가 성공해야 한다`() {
        val userCoupon = createUserCoupon()

        assertDoesNotThrow { userCoupon.assertUsableBy(USER_ID, Money(ORDER_AMOUNT)) }
    }

    @Test
    fun `타인의 쿠폰인 경우 assertUsableBy가 CouponException을 던져야 한다`() {
        val userCoupon = createUserCoupon()

        assertThatThrownBy { userCoupon.assertUsableBy(OTHER_USER_ID, Money(ORDER_AMOUNT)) }
            .isInstanceOf(CouponException::class.java)
    }

    @Test
    fun `USED 상태의 경우 assertUsableBy가 CouponException을 던져야 한다`() {
        val userCoupon = createUserCoupon().use()

        assertThatThrownBy { userCoupon.assertUsableBy(USER_ID, Money(ORDER_AMOUNT)) }
            .isInstanceOf(CouponException::class.java)
    }

    @Test
    fun `만료된 쿠폰의 경우 assertUsableBy가 CouponException을 던져야 한다`() {
        val coupon = createCoupon(expiredAt = PAST_DATE)
        val userCoupon = UserCoupon.reconstitute(
            persistenceId = 1L,
            refCouponId = 1L,
            refUserId = USER_ID,
            status = CouponStatus.AVAILABLE,
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            minOrderAmount = coupon.minOrderAmount,
            expiredAt = PAST_DATE,
            usedAt = null,
            issuedAt = ZonedDateTime.now(),
        )

        assertThatThrownBy { userCoupon.assertUsableBy(USER_ID, Money(ORDER_AMOUNT)) }
            .isInstanceOf(CouponException::class.java)
    }

    @Test
    fun `최소 주문 금액 미달의 경우 assertUsableBy가 CouponException을 던져야 한다`() {
        val userCoupon = createUserCoupon()

        assertThatThrownBy { userCoupon.assertUsableBy(USER_ID, Money(5000)) }
            .isInstanceOf(CouponException::class.java)
    }

    @Test
    fun `FIXED 할인의 경우 calculateDiscount가 할인 금액을 반환해야 한다`() {
        val userCoupon = createUserCoupon()

        val discount = userCoupon.calculateDiscount(Money(ORDER_AMOUNT))

        assertThat(discount.amount).isEqualTo(DISCOUNT_VALUE)
    }

    @Test
    fun `FIXED 할인이 주문 금액을 초과하면 주문 금액을 반환해야 한다`() {
        val coupon = createCoupon(discountValue = 50000)
        val userCoupon = UserCoupon.issue(coupon, USER_ID)

        val discount = userCoupon.calculateDiscount(Money(ORDER_AMOUNT))

        assertThat(discount.amount).isEqualTo(ORDER_AMOUNT)
    }

    @Test
    fun `RATE 할인의 경우 calculateDiscount가 비율 금액을 반환해야 한다`() {
        val coupon = Coupon.reconstitute(
            persistenceId = 1L,
            name = CouponName(COUPON_NAME),
            discountType = DiscountType.RATE,
            discountValue = 10,
            minOrderAmount = Money(MIN_ORDER_AMOUNT),
            maxIssueCount = MAX_ISSUE_COUNT,
            issuedCount = 0,
            expiredAt = FUTURE_DATE,
            deletedAt = null,
        )
        val userCoupon = UserCoupon.issue(coupon, USER_ID)

        val discount = userCoupon.calculateDiscount(Money(30000))

        assertThat(discount.amount).isEqualTo(3000)
    }

    private fun createCoupon(
        expiredAt: ZonedDateTime = FUTURE_DATE,
        discountValue: Long = DISCOUNT_VALUE,
    ): Coupon = Coupon.reconstitute(
        persistenceId = 1L,
        name = CouponName(COUPON_NAME),
        discountType = DiscountType.FIXED,
        discountValue = discountValue,
        minOrderAmount = Money(MIN_ORDER_AMOUNT),
        maxIssueCount = MAX_ISSUE_COUNT,
        issuedCount = 0,
        expiredAt = expiredAt,
        deletedAt = null,
    )

    private fun createUserCoupon(): UserCoupon {
        val coupon = createCoupon()
        return UserCoupon.issue(coupon, USER_ID)
    }

    companion object {
        private const val COUPON_NAME = "신규 가입 축하 쿠폰"
        private const val DISCOUNT_VALUE = 5000L
        private const val MIN_ORDER_AMOUNT = 10000L
        private const val MAX_ISSUE_COUNT = 100
        private const val ORDER_AMOUNT = 30000L
        private const val USER_ID = 1L
        private const val OTHER_USER_ID = 2L
        private val FUTURE_DATE: ZonedDateTime = ZonedDateTime.now().plusDays(30)
        private val PAST_DATE: ZonedDateTime = ZonedDateTime.now().minusDays(1)
    }
}
