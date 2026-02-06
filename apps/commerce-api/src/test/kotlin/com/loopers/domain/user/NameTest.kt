package com.loopers.domain.user

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NameTest {
    @Test
    fun `빈 값이면 실패한다`() {
        assertThrows<IllegalArgumentException> {
            Name("")
        }
    }

    @Nested
    @DisplayName("masked() 메서드는")
    inner class Masked {
        @Test
        fun `한글 이름의 마지막 글자를 마스킹한다`() {
            // given
            val name = Name("김철수")

            // when
            val masked = name.masked()

            // then
            assertThat(masked).isEqualTo("김철*")
        }

        @Test
        fun `영어 이름의 마지막 글자를 마스킹한다`() {
            // given
            val name = Name("John")

            // when
            val masked = name.masked()

            // then
            assertThat(masked).isEqualTo("Joh*")
        }

        @Test
        fun `한 글자 이름도 마스킹한다`() {
            // given
            val name = Name("김")

            // when
            val masked = name.masked()

            // then
            assertThat(masked).isEqualTo("*")
        }

        @Test
        fun `공백이 포함된 이름의 마지막 글자를 마스킹한다`() {
            // given
            val name = Name("김 철 수")

            // when
            val masked = name.masked()

            // then
            assertThat(masked).isEqualTo("김 철 *")
        }
    }
}
