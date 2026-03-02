package com.loopers.domain.brand

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class BusinessNumberTest {

    @Nested
    inner class Validation {
        @Test
        fun `사업자 번호 형식을 만족하면 통과한다`() {
            // 사업자 번호 형식은 XXX-XX-XXXXX
            assertDoesNotThrow {
                BusinessNumber("123-45-67890")
            }
        }

        @Test
        fun `형식을 만족하지 못하면 에러 반환한다`() {
            assertThrows<IllegalArgumentException> {
                BusinessNumber("12-45-67890")
            }

            assertThrows<IllegalArgumentException> {
                BusinessNumber("123-456-67890")
            }

            assertThrows<IllegalArgumentException> {
                BusinessNumber("123-45-6789")
            }

            assertThrows<IllegalArgumentException> {
                BusinessNumber("1234567890")
            }
        }
    }
}
