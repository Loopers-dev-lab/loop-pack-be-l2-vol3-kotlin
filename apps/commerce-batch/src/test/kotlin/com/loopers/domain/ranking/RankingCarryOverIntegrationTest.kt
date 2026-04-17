package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

@DisplayName("랭킹 Carry-Over 통합 테스트")
class RankingCarryOverIntegrationTest {

    private lateinit var fakeRepo: FakeCarryOverRepository
    private lateinit var service: RankingCarryOverService

    @BeforeEach
    fun setUp() {
        fakeRepo = FakeCarryOverRepository()
        service = RankingCarryOverService(fakeRepo, carryOverWeight = 0.1)
    }

    @DisplayName("전일 점수의 10%가 다음 날 키에 복사된다")
    @Test
    fun carriesTomorrowWith10Percent() {
        // arrange: 오늘 랭킹에 상품 2개
        val todayKey = RankingKeyGenerator.dailyKey(LocalDate.of(2026, 4, 10))
        fakeRepo.seedScore(todayKey, 101L, 100.0) // product:101 = 100점
        fakeRepo.seedScore(todayKey, 202L, 50.0) // product:202 = 50점

        // act
        val count = service.execute(LocalDate.of(2026, 4, 10))

        // assert
        val tomorrowKey = RankingKeyGenerator.dailyKey(LocalDate.of(2026, 4, 11))
        assertThat(count).isEqualTo(2)
        assertThat(fakeRepo.getScore(tomorrowKey, 101L))
            .isCloseTo(10.0, Offset.offset(0.0001)) // 100 * 0.1
        assertThat(fakeRepo.getScore(tomorrowKey, 202L))
            .isCloseTo(5.0, Offset.offset(0.0001)) // 50 * 0.1
    }

    @DisplayName("다음 날 키에 이미 점수가 있으면 carry-over 점수가 합산된다")
    @Test
    fun addsToExistingScoresInDestKey() {
        val todayKey = RankingKeyGenerator.dailyKey(LocalDate.of(2026, 4, 10))
        val tomorrowKey = RankingKeyGenerator.dailyKey(LocalDate.of(2026, 4, 11))

        fakeRepo.seedScore(todayKey, 101L, 100.0)
        // 내일 키에 이미 새벽 이벤트로 점수가 쌓인 경우
        fakeRepo.seedScore(tomorrowKey, 101L, 0.5)

        service.execute(LocalDate.of(2026, 4, 10))

        // 0.5 (기존) + 10.0 (carry-over) = 10.5
        assertThat(fakeRepo.getScore(tomorrowKey, 101L))
            .isCloseTo(10.5, Offset.offset(0.0001))
    }

    @DisplayName("원본 키가 비어있으면 아무것도 복사하지 않는다")
    @Test
    fun doesNothingWhenSourceEmpty() {
        val count = service.execute(LocalDate.of(2026, 4, 10))

        assertThat(count).isEqualTo(0)
        val tomorrowKey = RankingKeyGenerator.dailyKey(LocalDate.of(2026, 4, 11))
        assertThat(fakeRepo.getScore(tomorrowKey, 101L)).isNull()
    }

    @DisplayName("carry-over 후에도 원본 키의 점수는 변하지 않는다")
    @Test
    fun sourceKeyIsNotModified() {
        val todayKey = RankingKeyGenerator.dailyKey(LocalDate.of(2026, 4, 10))
        fakeRepo.seedScore(todayKey, 101L, 100.0)

        service.execute(LocalDate.of(2026, 4, 10))

        assertThat(fakeRepo.getScore(todayKey, 101L)).isEqualTo(100.0)
    }

    @DisplayName("carry-over 후 순위 순서가 유지된다 (원본과 동일한 상대 순위)")
    @Test
    fun relativeOrderIsPreserved() {
        val todayKey = RankingKeyGenerator.dailyKey(LocalDate.of(2026, 4, 10))
        fakeRepo.seedScore(todayKey, 101L, 100.0) // 1위
        fakeRepo.seedScore(todayKey, 202L, 80.0) // 2위
        fakeRepo.seedScore(todayKey, 303L, 30.0) // 3위

        service.execute(LocalDate.of(2026, 4, 10))

        val tomorrowKey = RankingKeyGenerator.dailyKey(LocalDate.of(2026, 4, 11))
        val score101 = fakeRepo.getScore(tomorrowKey, 101L)!!
        val score202 = fakeRepo.getScore(tomorrowKey, 202L)!!
        val score303 = fakeRepo.getScore(tomorrowKey, 303L)!!

        assertThat(score101).isGreaterThan(score202)
        assertThat(score202).isGreaterThan(score303)
    }

    /**
     * Fake Repository: carryOver 로직을 메모리에서 시뮬레이션
     */
    private class FakeCarryOverRepository : RankingCarryOverRepository {
        private val store = ConcurrentHashMap<String, ConcurrentHashMap<Long, Double>>()

        fun seedScore(key: String, productId: Long, score: Double) {
            store.computeIfAbsent(key) { ConcurrentHashMap() }[productId] = score
        }

        fun getScore(key: String, productId: Long): Double? {
            return store[key]?.get(productId)
        }

        override fun carryOver(sourceKey: String, destKey: String, carryOverWeight: Double): Long {
            val sourceMembers = store[sourceKey] ?: return 0
            if (sourceMembers.isEmpty()) return 0

            val destMembers = store.computeIfAbsent(destKey) { ConcurrentHashMap() }
            sourceMembers.forEach { (productId, score) ->
                destMembers.merge(productId, score * carryOverWeight, Double::plus)
            }
            return sourceMembers.size.toLong()
        }
    }
}
