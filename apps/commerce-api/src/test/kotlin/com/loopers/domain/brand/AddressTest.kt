package com.loopers.domain.brand

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class AddressTest {

    @Nested
    inner class Validation {
        @Test
        fun `주소 형식을 만족하면 통과한다`() {
            assertDoesNotThrow {
                Address("03045", "서울 종로구 효자로 12 국립고궁박물관", "광화문")
            }
        }

        @Test
        fun `우편번호 형식을 만족하지 못하면 에러 반환한다`() {
            assertThrows<IllegalArgumentException> {
                Address("0304", "서울 종로구 효자로 12 국립고궁박물관", "광화문")
            }
        }
    }
}
