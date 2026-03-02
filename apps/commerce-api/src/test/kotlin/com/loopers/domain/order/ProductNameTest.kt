package com.loopers.domain.order

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ProductNameTest {

    @Nested
    inner class Create {

        @Test
        fun `유효한 파라미터 입력 시 생성한다`() {
            assertDoesNotThrow {
                ProductName("뉴발란스 991")
            }
        }

        @Test
        fun `빈 값 입력 시 에러 반환한다`() {
            assertThrows<IllegalArgumentException> {
                ProductName("")
            }
        }

        @Test
        fun `공백만 입력 시 에러 반환한다`() {
            assertThrows<IllegalArgumentException> {
                ProductName("   ")
            }
        }
    }
}
