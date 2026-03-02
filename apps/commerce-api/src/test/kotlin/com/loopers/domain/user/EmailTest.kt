package com.loopers.domain.user

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class EmailTest {

    @Nested
    inner class Validation {
        @Test
        fun `이메일 형식을 만족하면 통과한다`() {
            assertDoesNotThrow {
                Email("test1234@loopers.com")
            }
        }

        @Test
        fun `특수문자는 점 언더바_ 퍼센트% 플러스+ 마이너스- 만 허용한다`() {
            assertThrows<IllegalArgumentException> {
                Email("test1234!@loopers.com")
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
        fun `최상위 도메인(TLD)가 없으면 실패한다`() {
            assertThrows<IllegalArgumentException> {
                Email("test1234@looper")
            }
        }

        @Test
        fun `최상위 도메인이 두글자 이상이 아니면 실패한다`() {
            assertThrows<IllegalArgumentException> {
                Email("test1234@loppers.c")
            }
        }

        @Test
        fun `빈 값이면 실패한다`() {
            assertThrows<IllegalArgumentException> {
                Email("")
            }
        }
    }
}
