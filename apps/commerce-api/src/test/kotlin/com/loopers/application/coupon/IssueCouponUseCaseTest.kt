package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponException
import com.loopers.domain.coupon.CouponName
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.fixture.FakeCouponRepository
import com.loopers.domain.coupon.fixture.FakeUserCouponRepository
import com.loopers.domain.product.Money
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class IssueCouponUseCaseTest {

    private lateinit var couponRepository: FakeCouponRepository
    private lateinit var userCouponRepository: FakeUserCouponRepository
    private lateinit var issueCouponUseCase: IssueCouponUseCase

    @BeforeEach
    fun setUp() {
        couponRepository = FakeCouponRepository()
        userCouponRepository = FakeUserCouponRepository()
        issueCouponUseCase = IssueCouponUseCase(couponRepository, userCouponRepository)
    }

    @Test
    fun `정상 발급의 경우 UserCoupon이 생성되고 ID를 반환해야 한다`() {
        val couponId = couponRepository.save(createCoupon())

        val result = issueCouponUseCase.issue(USER_ID, couponId)

        assertThat(result).isPositive()
    }

    @Test
    fun `발급 후 issuedCount가 증가해야 한다`() {
        val couponId = couponRepository.save(createCoupon())

        issueCouponUseCase.issue(USER_ID, couponId)

        val coupon = couponRepository.findById(couponId)!!
        assertThat(coupon.issuedCount).isEqualTo(1)
    }

    @Test
    fun `발급된 UserCoupon에 쿠폰 정보가 스냅샷되어야 한다`() {
        val couponId = couponRepository.save(createCoupon())

        val userCouponId = issueCouponUseCase.issue(USER_ID, couponId)

        val userCoupon = userCouponRepository.findById(userCouponId)!!
        assertThat(userCoupon.refCouponId).isEqualTo(couponId)
        assertThat(userCoupon.refUserId).isEqualTo(USER_ID)
        assertThat(userCoupon.discountType).isEqualTo(DiscountType.FIXED)
        assertThat(userCoupon.discountValue).isEqualTo(DISCOUNT_VALUE)
    }

    @Test
    fun `이미 발급받은 쿠폰을 재발급하면 CouponException이 발생해야 한다`() {
        val couponId = couponRepository.save(createCoupon())
        issueCouponUseCase.issue(USER_ID, couponId)

        assertThatThrownBy { issueCouponUseCase.issue(USER_ID, couponId) }
            .isInstanceOf(CouponException::class.java)
    }

    @Test
    fun `만료된 쿠폰을 발급하면 CouponException이 발생해야 한다`() {
        val expiredCoupon = createCoupon(expiredAt = ZonedDateTime.now().minusDays(1))
        val couponId = couponRepository.save(expiredCoupon)

        assertThatThrownBy { issueCouponUseCase.issue(USER_ID, couponId) }
            .isInstanceOf(CouponException::class.java)
    }

    @Test
    fun `발급 수량이 초과된 쿠폰을 발급하면 CouponException이 발생해야 한다`() {
        val fullCoupon = createCoupon(maxIssueCount = 1)
        val couponId = couponRepository.save(fullCoupon)
        issueCouponUseCase.issue(USER_ID, couponId)

        assertThatThrownBy { issueCouponUseCase.issue(OTHER_USER_ID, couponId) }
            .isInstanceOf(CouponException::class.java)
    }

    @Test
    fun `삭제된 쿠폰을 발급하면 CouponException이 발생해야 한다`() {
        val deletedCoupon = createCoupon().delete()
        val couponId = couponRepository.save(deletedCoupon)

        assertThatThrownBy { issueCouponUseCase.issue(USER_ID, couponId) }
            .isInstanceOf(CouponException::class.java)
    }

    private fun createCoupon(
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
        maxIssueCount: Int? = 100,
    ) = Coupon.create(
        name = CouponName(COUPON_NAME),
        discountType = DiscountType.FIXED,
        discountValue = DISCOUNT_VALUE,
        minOrderAmount = Money(MIN_ORDER_AMOUNT),
        maxIssueCount = maxIssueCount,
        expiredAt = expiredAt,
    )

    companion object {
        private const val USER_ID = 1L
        private const val OTHER_USER_ID = 2L
        private const val COUPON_NAME = "신규 가입 쿠폰"
        private const val DISCOUNT_VALUE = 3000L
        private const val MIN_ORDER_AMOUNT = 10000L
    }
}
