package com.loopers.domain.user

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class PasswordTest {

    @Nested
    inner class Validation {
        @Test
        fun `요구사항을 만족하면 통과한다`() {
            assertDoesNotThrow {
                Password("Loopers1234!@")
            }
        }

        @Test
        fun `빈 값인 경우 실패한다`() {
            assertThrows<IllegalArgumentException> {
                Password("")
            }
        }

        @Test
        fun `소문자 미포함 시 실패한다`() {
            assertThrows<IllegalArgumentException> {
                Password("TEST12!@")
            }
        }

        @Test
        fun `대문자 미포함 시 실패한다`() {
            assertThrows<IllegalArgumentException> {
                Password("test12!@")
            }
        }

        @Test
        fun `특수문자 미포함 시 실패한다`() {
            assertThrows<IllegalArgumentException> {
                Password("Test1234")
            }
        }

        @Test
        fun `8자 미만인 경우 실패한다`() {
            assertThrows<IllegalArgumentException> {
                Password("Test12!")
            }
        }

        @Test
        fun `16자 초과인 경우 실패한다`() {
            assertThrows<IllegalArgumentException> {
                Password("Test12345678!@#$%^&*")
            }
        }
    }
}
