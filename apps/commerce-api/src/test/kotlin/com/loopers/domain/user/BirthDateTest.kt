package com.loopers.domain.user

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class BirthDateTest {

    @Test
    fun `YYYY-MM-DD 형식을 만족하면 통과한다`() {
        assertDoesNotThrow {
            BirthDate("2002-01-01")
        }
    }

    @Test
    fun `YY-MM-DD 형식인 경우 실패한다`() {
        assertThrows<IllegalArgumentException> {
            BirthDate("02-01-01")
        }
    }
}
