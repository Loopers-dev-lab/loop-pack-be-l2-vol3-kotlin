package com.loopers.domain.brand

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class PhoneNumberTest {
    @Nested
    inner class Validation {
        @Test
        fun `전화번호 형식을 만족하면 통과한다`() {
            assertDoesNotThrow {
                PhoneNumber("010-1234-5678")
            }

            assertDoesNotThrow {
                PhoneNumber("02-123-4567")
            }

            assertDoesNotThrow {
                PhoneNumber("051-123-4567")
            }
        }

        @Test
        fun `형식을 만족하지 못하면 에러 반환한다`() {
            assertThrows<IllegalArgumentException> {
                PhoneNumber("0101-1234-5678")
            }

            assertThrows<IllegalArgumentException> {
                PhoneNumber("010-12345-5678")
            }

            assertThrows<IllegalArgumentException> {
                PhoneNumber("010-1234-56789")
            }

            assertThrows<IllegalArgumentException> {
                PhoneNumber("02-12345-5678")
            }

            assertThrows<IllegalArgumentException> {
                PhoneNumber("02-1234-56789")
            }

            assertThrows<IllegalArgumentException> {
                PhoneNumber("0-1234-5678")
            }

            assertThrows<IllegalArgumentException> {
                PhoneNumber("01012345678")
            }
        }
    }
}
