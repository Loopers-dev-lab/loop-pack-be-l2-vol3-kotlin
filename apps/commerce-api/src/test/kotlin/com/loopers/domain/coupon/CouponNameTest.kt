package com.loopers.domain.coupon

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class CouponNameTest {

    @Nested
    inner class Create {

        @Test
        fun `유효한 이름으로 생성한다`() {
            assertDoesNotThrow {
                CouponName("Spring Sale")
            }
        }

        @Test
        fun `공백 문자열 입력 시 에러를 반환한다`() {
            assertThrows<IllegalArgumentException> {
                CouponName(" ")
            }
        }

        @Test
        fun `빈 문자열 입력 시 에러를 반환한다`() {
            assertThrows<IllegalArgumentException> {
                CouponName("")
            }
        }

        @Test
        fun `100자 이름으로 생성한다`() {
            assertDoesNotThrow {
                CouponName("a".repeat(100))
            }
        }

        @Test
        fun `101자 이름 입력 시 에러를 반환한다`() {
            assertThrows<IllegalArgumentException> {
                CouponName("a".repeat(101))
            }
        }
    }
}
