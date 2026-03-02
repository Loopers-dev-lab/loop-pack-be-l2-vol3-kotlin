package com.loopers.domain.order

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class TotalAmountTest {

    @Nested
    inner class Create {

        @Test
        fun `유효한 파라미터 입력 시 생성한다`() {
            assertDoesNotThrow {
                TotalAmount(1000)
            }
        }

        @Test
        fun `0 입력 시 에러 반환한다`() {
            assertThrows<IllegalArgumentException> {
                TotalAmount(0)
            }
        }

        @Test
        fun `음수 입력 시 에러 반환한다`() {
            assertThrows<IllegalArgumentException> {
                TotalAmount(-1)
            }
        }
    }
}
