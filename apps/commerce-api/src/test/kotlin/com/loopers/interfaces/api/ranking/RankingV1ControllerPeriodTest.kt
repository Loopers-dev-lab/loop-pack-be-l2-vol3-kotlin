package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingPeriod
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("RankingPeriod 파라미터 유효성 검증 테스트")
class RankingV1ControllerPeriodTest {

    @Test
    @DisplayName("period 미지정 시 기본값 daily가 적용된다")
    fun `period 기본값은 daily`() {
        val result = RankingPeriod.fromOrNull("daily")
        assertThat(result).isEqualTo(RankingPeriod.DAILY)
    }

    @ParameterizedTest
    @ValueSource(strings = ["WEEKLY", "weekly", "Weekly", "wEeKlY"])
    @DisplayName("period=weekly는 대소문자 무관하게 WEEKLY로 파싱된다")
    fun `weekly 대소문자 무관 파싱`(input: String) {
        val result = RankingPeriod.fromOrNull(input)
        assertThat(result).isEqualTo(RankingPeriod.WEEKLY)
    }

    @ParameterizedTest
    @ValueSource(strings = ["MONTHLY", "monthly", "Monthly"])
    @DisplayName("period=monthly는 대소문자 무관하게 MONTHLY로 파싱된다")
    fun `monthly 대소문자 무관 파싱`(input: String) {
        val result = RankingPeriod.fromOrNull(input)
        assertThat(result).isEqualTo(RankingPeriod.MONTHLY)
    }

    @Test
    @DisplayName("period=invalid는 null을 반환한다")
    fun `잘못된 period는 null 반환`() {
        val result = RankingPeriod.fromOrNull("invalid")
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("period=null이면 null을 반환한다")
    fun `null period는 null 반환`() {
        val result = RankingPeriod.fromOrNull(null)
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("fromOrNull이 null이면 CoreException BAD_REQUEST가 발생한다")
    fun `잘못된 period는 BAD_REQUEST CoreException을 발생시킨다`() {
        val invalidPeriod = RankingPeriod.fromOrNull("wrong")
        val exception = assertThrows<CoreException> {
            invalidPeriod ?: throw CoreException(ErrorType.BAD_REQUEST, "period는 daily|weekly|monthly 중 하나여야 합니다.")
        }
        assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
    }
}
