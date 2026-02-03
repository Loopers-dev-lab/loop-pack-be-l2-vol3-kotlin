package com.loopers.domain.user

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class LoginIdTest {

    @Test
    fun `영문과 숫자로만 이루어지면 성공한다` () {

        assertDoesNotThrow {
            LoginId("test123")
        }
    }

    @Test
    fun `빈 값인 경우 실패한다` () {

        assertThrows<IllegalArgumentException> {
            LoginId("")
        }
    }

    @Test
    fun `공백이 있는 경우 실패한다` () {

        assertThrows<IllegalArgumentException> {
            LoginId("test 123")
        }
    }

    @Test
    fun `특수 문자가 있는 경우 실패한다` () {

        assertThrows<IllegalArgumentException> {
            LoginId("test123!@#")
        }
    }
}
