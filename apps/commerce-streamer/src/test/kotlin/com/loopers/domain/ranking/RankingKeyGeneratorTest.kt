package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("RankingKeyGenerator")
class RankingKeyGeneratorTest {

    @DisplayName("dailyKey는 날짜를 yyyyMMdd 형식으로 키에 포함한다")
    @Test
    fun generatesDailyKey() {
        val date = LocalDate.of(2026, 4, 10)

        val key = RankingKeyGenerator.dailyKey(date)

        assertThat(key).isEqualTo("ranking:all:20260410")
    }

    @DisplayName("서로 다른 날짜는 서로 다른 키를 생성한다")
    @Test
    fun differentDatesProduceDifferentKeys() {
        val today = LocalDate.of(2026, 4, 10)
        val yesterday = LocalDate.of(2026, 4, 9)

        assertThat(RankingKeyGenerator.dailyKey(today))
            .isNotEqualTo(RankingKeyGenerator.dailyKey(yesterday))
    }
}
