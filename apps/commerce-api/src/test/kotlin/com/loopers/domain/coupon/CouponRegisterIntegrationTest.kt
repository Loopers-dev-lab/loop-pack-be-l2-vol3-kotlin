package com.loopers.domain.coupon

import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZonedDateTime

@SpringBootTest
class CouponRegisterIntegrationTest @Autowired constructor(
    private val couponRegister: CouponRegister,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    inner class Register {
        @Test
        fun `정액_할인_쿠폰을_등록할_수_있다`() {
            // act
            val coupon = couponRegister.register(
                name = "신규가입 쿠폰",
                type = CouponType.FIXED,
                discountValue = 3000L,
                minOrderAmount = 10000L,
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // assert
            assertAll(
                { assertThat(coupon.id).isNotNull() },
                { assertThat(coupon.id).isGreaterThan(0) },
                { assertThat(coupon.name.value).isEqualTo("신규가입 쿠폰") },
                { assertThat(coupon.type).isEqualTo(CouponType.FIXED) },
                { assertThat(coupon.discountValue.value).isEqualTo(3000L) },
                { assertThat(coupon.minOrderAmount.value).isEqualTo(10000L) },
            )
        }

        @Test
        fun `정률_할인_쿠폰을_등록할_수_있다`() {
            // act
            val coupon = couponRegister.register(
                name = "10% 할인 쿠폰",
                type = CouponType.RATE,
                discountValue = 10L,
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // assert
            assertAll(
                { assertThat(coupon.id).isNotNull() },
                { assertThat(coupon.type).isEqualTo(CouponType.RATE) },
                { assertThat(coupon.discountValue.value).isEqualTo(10L) },
                { assertThat(coupon.minOrderAmount.value).isNull() },
            )
        }
    }
}
