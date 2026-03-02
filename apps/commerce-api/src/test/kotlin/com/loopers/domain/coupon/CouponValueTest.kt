package com.loopers.domain.coupon

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class CouponValueTest {

    @Nested
    inner class Create {

        @Test
        fun `양수 입력 시 생성한다`() {
            assertDoesNotThrow {
                CouponValue(1L)
            }
        }

        @Test
        fun `0 입력 시 에러를 반환한다`() {
            assertThrows<IllegalArgumentException> {
                CouponValue(0L)
            }
        }

        @Test
        fun `음수 입력 시 에러를 반환한다`() {
            assertThrows<IllegalArgumentException> {
                CouponValue(-1L)
            }
        }
    }
}
