package com.loopers.domain.user

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NameTest {

    @Nested
    inner class Validation {
        @Test
        fun `빈 값이면 실패한다`() {
            assertThrows<IllegalArgumentException> {
                Name("")
            }
        }

        @Test
        fun `이름 앞에 공백이 있으면 실패한다`() {
            assertThrows<IllegalArgumentException> {
                Name(" 홍길동")
            }
        }

        @Test
        fun `이름 뒤에 공백이 있으면 실패한다`() {
            assertThrows<IllegalArgumentException> {
                Name("홍길동 ")
            }
        }

        @Test
        fun `이름 사이에 공백이 연속해서 있으면 실패한다`() {
            assertThrows<IllegalArgumentException> {
                Name("홍  길동")
            }
        }
    }

    @Nested
    @DisplayName("masked() 메서드는")
    inner class Masked {
        @Test
        fun `한글 이름의 마지막 글자를 마스킹한다`() {
            val name = Name("홍길동")

            val masked = name.masked()

            assertThat(masked).isEqualTo("홍길*")
        }

        @Test
        fun `영어 이름의 마지막 글자를 마스킹한다`() {
            val name = Name("John")

            val masked = name.masked()

            assertThat(masked).isEqualTo("Joh*")
        }

        @Test
        fun `한 글자 이름도 마스킹한다`() {
            val name = Name("홍")

            val masked = name.masked()

            assertThat(masked).isEqualTo("*")
        }

        @Test
        fun `공백이 포함된 이름의 마지막 글자를 마스킹한다`() {
            val name = Name("김 철 수")

            val masked = name.masked()

            assertThat(masked).isEqualTo("김 철 *")
        }
    }
}
