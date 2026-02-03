package com.loopers.domain.user

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NameTest {
    @Test
    fun `빈 값이면 실패한다` () {

        assertThrows<IllegalArgumentException> {
            Name("")
        }
    }
}
