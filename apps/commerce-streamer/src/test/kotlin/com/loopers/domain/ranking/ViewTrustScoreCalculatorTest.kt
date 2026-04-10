package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ViewTrustScoreCalculatorTest {

    private val calculator = ViewTrustScoreCalculator()

    @DisplayName("Trust Score를 계산할 때,")
    @Nested
    inner class Calculate {

        @DisplayName("정상 로그인 유저는 최고 점수(1.0)를 받는다.")
        @Test
        fun returnsMaxScore_whenNormalLoggedInUser() {
            // arrange
            val signals = ViewSignals(
                isLoggedIn = true,
                hasUserAgent = true,
                hasReferer = true,
                requestsPerMinute = 1,
                distinctProductsIn10Min = 5,
            )

            // act
            val score = calculator.calculate(signals)

            // assert
            assertThat(score).isEqualTo(1.0)
        }

        @DisplayName("정상 비로그인 유저는 0.75점을 받는다.")
        @Test
        fun returnsModerateScore_whenNormalGuest() {
            // arrange
            val signals = ViewSignals(
                isLoggedIn = false,
                hasUserAgent = true,
                hasReferer = true,
                requestsPerMinute = 2,
                distinctProductsIn10Min = 3,
            )

            // act
            val score = calculator.calculate(signals)

            // assert — guest(0.05) + ua(0.1) + referer(0.1) + rate(0.3) + diversity(0.2) = 0.75
            assertThat(score).isEqualTo(0.75)
        }

        @DisplayName("의심 봇(UA 없음, 분당 15회, 단일 상품)은 0.05점을 받는다.")
        @Test
        fun returnsMinimalScore_whenSuspiciousBot() {
            // arrange
            val signals = ViewSignals(
                isLoggedIn = false,
                hasUserAgent = false,
                hasReferer = false,
                requestsPerMinute = 15,
                distinctProductsIn10Min = 1,
            )

            // act
            val score = calculator.calculate(signals)

            // assert — guest(0.05) + ua(0) + referer(0) + rate(0) + diversity(0) = 0.05
            assertThat(score).isEqualTo(0.05)
        }

        @DisplayName("점수는 0.0 ~ 1.0 범위로 제한된다.")
        @Test
        fun clampsScore_withinRange() {
            // arrange — 모든 시그널 최대
            val signals = ViewSignals(
                isLoggedIn = true,
                hasUserAgent = true,
                hasReferer = true,
                requestsPerMinute = 1,
                distinctProductsIn10Min = 10,
            )

            // act
            val score = calculator.calculate(signals)

            // assert
            assertThat(score).isLessThanOrEqualTo(1.0)
            assertThat(score).isGreaterThanOrEqualTo(0.0)
        }
    }
}
