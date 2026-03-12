package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZonedDateTime

@SpringBootTest
class RegisterCouponUseCaseTest @Autowired constructor(
    private val registerCouponUseCase: RegisterCouponUseCase,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("쿠폰 등록")
    @Nested
    inner class Execute {

        @DisplayName("정액 쿠폰을 등록하면 성공한다")
        @Test
        fun successFixed() {
            val result = registerCouponUseCase.execute(
                CouponCommand.Register(
                    name = "1000원 할인",
                    type = CouponType.FIXED,
                    value = 1000,
                    minOrderAmount = 10000,
                    expiredAt = ZonedDateTime.now().plusDays(30),
                ),
            )

            assertAll(
                { assertThat(result.id).isGreaterThan(0) },
                { assertThat(result.name).isEqualTo("1000원 할인") },
                { assertThat(result.type).isEqualTo("FIXED") },
                { assertThat(result.value).isEqualTo(1000) },
                { assertThat(result.minOrderAmount).isEqualTo(10000) },
            )
        }

        @DisplayName("정률 쿠폰을 등록하면 성공한다")
        @Test
        fun successRate() {
            val result = registerCouponUseCase.execute(
                CouponCommand.Register(
                    name = "10% 할인",
                    type = CouponType.RATE,
                    value = 10,
                    minOrderAmount = null,
                    expiredAt = ZonedDateTime.now().plusDays(30),
                ),
            )

            assertAll(
                { assertThat(result.type).isEqualTo("RATE") },
                { assertThat(result.value).isEqualTo(10) },
                { assertThat(result.minOrderAmount).isNull() },
            )
        }
    }
}
