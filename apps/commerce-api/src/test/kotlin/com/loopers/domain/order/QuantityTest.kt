package com.loopers.domain.order

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class QuantityTest {

    @Nested
    inner class Create {

        @Test
        fun `유효한 파라미터 입력 시 생성한다`() {
            assertDoesNotThrow {
                Quantity(1)
            }
        }

        @Test
        fun `0 입력 시 에러 반환한다`() {
            assertThrows<IllegalArgumentException> {
                Quantity(0)
            }
        }

        @Test
        fun `음수 입력 시 에러 반환한다`() {
            assertThrows<IllegalArgumentException> {
                Quantity(-1)
            }
        }
    }
}
