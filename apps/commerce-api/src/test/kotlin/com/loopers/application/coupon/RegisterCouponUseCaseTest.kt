package com.loopers.application.coupon

import com.loopers.domain.coupon.fixture.FakeCouponRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class RegisterCouponUseCaseTest {

    private lateinit var couponRepository: FakeCouponRepository
    private lateinit var registerCouponUseCase: RegisterCouponUseCase

    @BeforeEach
    fun setUp() {
        couponRepository = FakeCouponRepository()
        registerCouponUseCase = RegisterCouponUseCase(couponRepository)
    }

    @Test
    fun `정상 요청의 경우 쿠폰이 등록되고 ID를 반환해야 한다`() {
        val command = createCommand()

        val result = registerCouponUseCase.register(command)

        assertThat(result).isPositive()
    }

    @Test
    fun `등록된 쿠폰의 정보가 올바르게 저장되어야 한다`() {
        val command = createCommand()

        val id = registerCouponUseCase.register(command)

        val coupon = couponRepository.findById(id)!!
        assertThat(coupon.name.value).isEqualTo(COUPON_NAME)
        assertThat(coupon.discountType.name).isEqualTo("FIXED")
        assertThat(coupon.discountValue).isEqualTo(DISCOUNT_VALUE)
        assertThat(coupon.minOrderAmount.amount).isEqualTo(MIN_ORDER_AMOUNT)
        assertThat(coupon.maxIssueCount).isEqualTo(MAX_ISSUE_COUNT)
        assertThat(coupon.issuedCount).isEqualTo(0)
    }

    @Test
    fun `잘못된 discountType의 경우 IllegalArgumentException이 발생해야 한다`() {
        val command = createCommand(discountType = "INVALID")

        assertThatThrownBy { registerCouponUseCase.register(command) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun createCommand(
        discountType: String = "FIXED",
    ) = RegisterCouponCommand(
        name = COUPON_NAME,
        discountType = discountType,
        discountValue = DISCOUNT_VALUE,
        minOrderAmount = MIN_ORDER_AMOUNT,
        maxIssueCount = MAX_ISSUE_COUNT,
        expiredAt = ZonedDateTime.now().plusDays(30),
    )

    companion object {
        private const val COUPON_NAME = "신규 가입 쿠폰"
        private const val DISCOUNT_VALUE = 3000L
        private const val MIN_ORDER_AMOUNT = 10000L
        private const val MAX_ISSUE_COUNT = 100
    }
}
