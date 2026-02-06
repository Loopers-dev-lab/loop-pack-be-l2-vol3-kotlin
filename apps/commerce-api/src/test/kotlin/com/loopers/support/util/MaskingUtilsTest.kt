package com.loopers.support.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("MaskingUtils")
class MaskingUtilsTest {

    @DisplayName("maskName")
    @Nested
    inner class MaskName {
        @DisplayName("이름 마스킹 규칙에 따라 마스킹된다")
        @ParameterizedTest
        @CsvSource(
            "홍길동, 홍*동",
            "김철, 김*",
            "John Doe, J******e",
        )
        fun masksNameCorrectly(input: String, expected: String) {
            // act
            val result = MaskingUtils.maskName(input)

            // assert
            assertThat(result).isEqualTo(expected)
        }

        @DisplayName("1글자 이름은 그대로 반환된다")
        @Test
        fun returnsSingleCharAsIs() {
            // act
            val result = MaskingUtils.maskName("홍")

            // assert
            assertThat(result).isEqualTo("홍")
        }
    }

    @DisplayName("maskEmail")
    @Nested
    inner class MaskEmail {
        @DisplayName("이메일 마스킹 규칙에 따라 마스킹된다")
        @ParameterizedTest
        @CsvSource(
            "hong@example.com, ho***@example.com",
            "a@b.com, a***@b.com",
            "example@test.co.kr, ex***@test.co.kr",
        )
        fun masksEmailCorrectly(input: String, expected: String) {
            // act
            val result = MaskingUtils.maskEmail(input)

            // assert
            assertThat(result).isEqualTo(expected)
        }
    }
}
