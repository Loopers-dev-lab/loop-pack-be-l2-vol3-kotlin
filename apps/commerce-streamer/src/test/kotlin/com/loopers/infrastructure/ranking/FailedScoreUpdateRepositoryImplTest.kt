package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.model.FailedScoreUpdate
import com.loopers.domain.ranking.repository.FailedScoreUpdateRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class FailedScoreUpdateRepositoryImplTest @Autowired constructor(
    private val failedScoreUpdateRepository: FailedScoreUpdateRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("동일 createdAt 레코드가 limit보다 많아도 id 오름차순으로 안정 정렬되며, 2회 연속 호출 시 동일 결과를 반환한다")
    fun `동일 createdAt 안정 정렬 검증`() {
        // Arrange — 동일 createdAt 5건 저장 (limit=3보다 많음)
        val createdAt = ZonedDateTime.of(2026, 4, 10, 12, 0, 0, 0, ZoneId.of("Asia/Seoul"))
        val rankingDate = LocalDate.of(2026, 4, 10)
        val savedIds = (1..5).map { i ->
            failedScoreUpdateRepository.save(
                FailedScoreUpdate(
                    eventId = "evt-$i",
                    productId = i.toLong(),
                    score = 0.2,
                    rankingDate = rankingDate,
                    createdAt = createdAt,
                ),
            ).id
        }

        // Act — limit=3으로 2회 연속 조회
        val firstCall = failedScoreUpdateRepository.findPendingUpdates(maxRetryCount = 10, limit = 3)
        val secondCall = failedScoreUpdateRepository.findPendingUpdates(maxRetryCount = 10, limit = 3)

        // Assert — id 오름차순으로 안정 정렬, 반복 호출 시 결과 동일
        val expectedIds = savedIds.sorted().take(3)
        assertThat(firstCall.map { it.id }).containsExactlyElementsOf(expectedIds)
        assertThat(secondCall.map { it.id }).containsExactlyElementsOf(expectedIds)
    }

    @Test
    @DisplayName("서로 다른 createdAt이면 createdAt 오름차순이 1차 기준이다")
    fun `createdAt 오름차순 1차 정렬`() {
        // Arrange — createdAt 다른 3건
        val base = ZonedDateTime.of(2026, 4, 10, 12, 0, 0, 0, ZoneId.of("Asia/Seoul"))
        val rankingDate = LocalDate.of(2026, 4, 10)
        val older = failedScoreUpdateRepository.save(
            FailedScoreUpdate(
                eventId = "evt-older",
                productId = 10L,
                score = 0.2,
                rankingDate = rankingDate,
                createdAt = base.minusMinutes(10),
            ),
        )
        val newer = failedScoreUpdateRepository.save(
            FailedScoreUpdate(
                eventId = "evt-newer",
                productId = 20L,
                score = 0.2,
                rankingDate = rankingDate,
                createdAt = base,
            ),
        )

        // Act
        val result = failedScoreUpdateRepository.findPendingUpdates(maxRetryCount = 10, limit = 10)

        // Assert — 오래된 것이 먼저
        assertThat(result.map { it.id }).containsExactly(older.id, newer.id)
    }
}
