package com.loopers.domain.user

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class BirthDateTest {

    @Test
    fun `0000-00-00 형식을 만족하면 통과한다` () {

        assertDoesNotThrow {
            BirthDate("1993-04-28")
        }
    }

    @Test
    fun `00-00-00 형식인 경우 실패한다` () {

        assertThrows<IllegalArgumentException> {
            BirthDate("93-04-28")
        }
    }
}
