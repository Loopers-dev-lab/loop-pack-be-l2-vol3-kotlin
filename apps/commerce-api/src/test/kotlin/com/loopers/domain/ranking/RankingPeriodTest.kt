package com.loopers.domain.ranking

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RankingPeriodTest {

    @Nested
    @DisplayName("from - 유효한 소문자 입력")
    inner class ValidLowercase {

        @Test
        @DisplayName("'daily'를 입력하면 DAILY를 반환한다")
        fun `daily는 DAILY를 반환한다`() {
            assertThat(RankingPeriod.from("daily")).isEqualTo(RankingPeriod.DAILY)
        }

        @Test
        @DisplayName("'weekly'를 입력하면 WEEKLY를 반환한다")
        fun `weekly는 WEEKLY를 반환한다`() {
            assertThat(RankingPeriod.from("weekly")).isEqualTo(RankingPeriod.WEEKLY)
        }

        @Test
        @DisplayName("'monthly'를 입력하면 MONTHLY를 반환한다")
        fun `monthly는 MONTHLY를 반환한다`() {
            assertThat(RankingPeriod.from("monthly")).isEqualTo(RankingPeriod.MONTHLY)
        }
    }

    @Nested
    @DisplayName("from - 유효하지 않은 입력")
    inner class InvalidInput {

        @ParameterizedTest
        @ValueSource(strings = ["Daily", "DAILY", "Monthly", "WEEKLY", "Weekly", "", "unknown"])
        @DisplayName("소문자가 아니거나 알 수 없는 값이면 BAD_REQUEST CoreException을 던진다")
        fun `유효하지 않은 값은 BAD_REQUEST CoreException을 던진다`(value: String) {
            val ex = assertThrows<CoreException> {
                RankingPeriod.from(value)
            }
            assertThat(ex.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
