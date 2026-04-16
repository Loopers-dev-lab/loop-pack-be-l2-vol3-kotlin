package com.loopers.batch.job.ranking

import com.loopers.batch.infrastructure.ranking.MvProductRankWeeklyJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.batch.item.Chunk
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.util.ReflectionTestUtils

class WeeklyRankingChunkWriterTest {

    private val weeklyRepository: MvProductRankWeeklyJpaRepository = mock()
    private val jdbcTemplate: JdbcTemplate = mock()
    private lateinit var writer: WeeklyRankingChunkWriter

    @BeforeEach
    fun setUp() {
        writer = WeeklyRankingChunkWriter(weeklyRepository, jdbcTemplate)
        // 2026-04-14 (화요일) → ISO 주차: 2026-W16
        ReflectionTestUtils.setField(writer, "targetDate", "20260414")
    }

    @DisplayName("첫 번째 청크: 기존 주간 데이터 삭제 후 upsert 수행")
    @Test
    fun shouldDeleteExistingDataOnFirstChunk() {
        val chunk = Chunk(listOf(RankingScoreContribution(productId = 1L, score = 70.0)))

        writer.write(chunk)

        verify(weeklyRepository).deleteByYearWeek("2026-W16")
        verify(jdbcTemplate).batchUpdate(any<String>(), any<List<Array<Any>>>())
    }

    @DisplayName("두 번째 청크: 삭제 없이 upsert만 수행")
    @Test
    fun shouldNotDeleteOnSubsequentChunks() {
        val firstChunk = Chunk(listOf(RankingScoreContribution(productId = 1L, score = 70.0)))
        val secondChunk = Chunk(listOf(RankingScoreContribution(productId = 2L, score = 50.0)))

        writer.write(firstChunk)
        writer.write(secondChunk)

        verify(weeklyRepository, times(1)).deleteByYearWeek("2026-W16")
        verify(jdbcTemplate, times(2)).batchUpdate(any<String>(), any<List<Array<Any>>>())
    }

    @DisplayName("upsert 호출 시 productId, yearWeek, score가 올바르게 전달됨")
    @Test
    fun shouldPassCorrectArgsToUpsert() {
        val chunk = Chunk(
            listOf(
                RankingScoreContribution(productId = 10L, score = 75.5),
                RankingScoreContribution(productId = 20L, score = 50.0),
            ),
        )

        writer.write(chunk)

        val sqlCaptor = argumentCaptor<String>()
        val argsCaptor = argumentCaptor<List<Array<Any>>>()
        verify(jdbcTemplate).batchUpdate(sqlCaptor.capture(), argsCaptor.capture())

        val batchArgs = argsCaptor.firstValue
        assertThat(batchArgs).hasSize(2)

        val first = batchArgs[0]
        assertThat(first[0]).isEqualTo(10L) // productId
        assertThat(first[1]).isEqualTo("2026-W16") // yearWeek
        assertThat(first[2]).isEqualTo(75.5) // score
    }

    @DisplayName("청크 내 아이템이 없으면 upsert 호출하지 않음")
    @Test
    fun shouldNotUpsertOnEmptyChunk() {
        val chunk = Chunk(emptyList<RankingScoreContribution>())

        writer.write(chunk)

        verify(jdbcTemplate, never()).batchUpdate(any<String>(), any<List<Array<Any>>>())
    }

    @DisplayName("yearWeek 형식: targetDate가 해당 주 어떤 요일이든 올바른 주차로 변환")
    @Test
    fun shouldConvertAnyDayOfWeekToCorrectYearWeek() {
        // 2026-04-13 (월요일)도 같은 주 W16
        ReflectionTestUtils.setField(writer, "targetDate", "20260413")
        // isFirstChunk를 다시 true로 설정
        ReflectionTestUtils.setField(writer, "isFirstChunk", true)

        writer.write(Chunk(listOf(RankingScoreContribution(1L, 100.0))))

        verify(weeklyRepository).deleteByYearWeek(eq("2026-W16"))
    }
}
