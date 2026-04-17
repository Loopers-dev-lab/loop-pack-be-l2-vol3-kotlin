package com.loopers.domain.ranking

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("RankingCarryOverService")
class RankingCarryOverServiceTest {

    private val repository: RankingCarryOverRepository = mockk()
    private val service = RankingCarryOverService(repository, carryOverWeight = 0.1)

    @DisplayName("오늘 키에서 내일 키로 carryOverWeight를 적용하여 복사한다")
    @Test
    fun carriesOverTodayToTomorrow() {
        // arrange
        val baseDate = LocalDate.of(2026, 4, 10)
        every {
            repository.carryOver("ranking:all:20260410", "ranking:all:20260411", 0.1)
        } returns 5L

        // act
        val count = service.execute(baseDate)

        // assert
        assertThat(count).isEqualTo(5)
        verify(exactly = 1) {
            repository.carryOver("ranking:all:20260410", "ranking:all:20260411", 0.1)
        }
    }

    @DisplayName("원본 키가 비어있으면 0을 반환한다")
    @Test
    fun returnsZeroWhenSourceIsEmpty() {
        val baseDate = LocalDate.of(2026, 4, 10)
        every {
            repository.carryOver("ranking:all:20260410", "ranking:all:20260411", 0.1)
        } returns 0L

        val count = service.execute(baseDate)

        assertThat(count).isEqualTo(0)
    }
}
