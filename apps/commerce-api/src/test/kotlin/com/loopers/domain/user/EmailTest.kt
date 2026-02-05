package com.loopers.domain.user

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class EmailTest {

    @Test
    fun `이메일 형식을 만족하면 통과한다`() {
        assertDoesNotThrow {
            Email("test1234@loopers.com")
        }
    }

    @Test
    fun `@가 없으면 실패한다`() {
        assertThrows<IllegalArgumentException> {
            Email("test1234loopers.com")
        }
    }

    @Test
    fun `도메인이 없으면 실패한다`() {
        assertThrows<IllegalArgumentException> {
            Email("test1234@")
        }
    }

    @Test
    fun `빈 값이면 실패한다`() {
        assertThrows<IllegalArgumentException> {
            Email("")
        }
    }
}
