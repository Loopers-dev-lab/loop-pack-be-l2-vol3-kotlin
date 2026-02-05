package com.loopers.support.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MaskingUtilsTest {

    @DisplayName("마지막 글자 마스킹")
    @Nested
    inner class MaskLastCharacter {

        @DisplayName("문자열의 마지막 글자가 *로 대체된다.")
        @Test
        fun maskLastCharacter() {
            // act
            val result = MaskingUtils.maskLastCharacter("홍길동")

            // assert
            assertThat(result).isEqualTo("홍길*")
        }

        @DisplayName("2글자면 마지막 글자만 마스킹된다.")
        @Test
        fun maskTwoCharacterString() {
            // act
            val result = MaskingUtils.maskLastCharacter("홍길")

            // assert
            assertThat(result).isEqualTo("홍*")
        }

        @DisplayName("1글자면 *로 대체된다.")
        @Test
        fun maskSingleCharacter() {
            // act
            val result = MaskingUtils.maskLastCharacter("홍")

            // assert
            assertThat(result).isEqualTo("*")
        }

        @DisplayName("빈 문자열이면 *로 대체된다.")
        @Test
        fun maskEmptyString() {
            // act
            val result = MaskingUtils.maskLastCharacter("")

            // assert
            assertThat(result).isEqualTo("*")
        }
    }
}
