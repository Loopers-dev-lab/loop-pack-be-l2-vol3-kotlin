package com.loopers.domain.product

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class StockTest {

    @Test
    fun `유효한 파라미터 입력 시 생성한다`() {
        assertDoesNotThrow {
            Stock(100L)
        }
    }

    @Test
    fun `음수 입력 시 에러 반환한다`() {
        assertThrows<IllegalArgumentException> {
            Stock(-100L)
        }
    }
}
