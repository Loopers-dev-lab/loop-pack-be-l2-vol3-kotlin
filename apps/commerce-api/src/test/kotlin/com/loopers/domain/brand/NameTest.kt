package com.loopers.domain.brand

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NameTest {

    @Nested
    inner class Validation {
        @Test
        fun `빈 값이면 에러 반환한다`() {
            assertThrows<IllegalArgumentException> {
                Name("")
            }
        }
    }
}
