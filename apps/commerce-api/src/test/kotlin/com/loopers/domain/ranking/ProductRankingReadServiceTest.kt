package com.loopers.domain.ranking

import com.loopers.interfaces.api.ranking.RankingPeriod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

class ProductRankingReadServiceTest {

    private val productRankingRepository: ProductRankingRepository = mock()
    private val mvProductRankRepository: MvProductRankRepository = mock()
    private val service = ProductRankingReadService(productRankingRepository, mvProductRankRepository)

    @DisplayName("DAILY: productRankingRepository에서 조회")
    @Test
    fun shouldQueryRedisForDaily() {
        // arrange
        val date = LocalDate.of(2026, 4, 14)
        val rankedProduct = ProductRankingReadModel(productId = 1L, rank = 1L, score = 1000.0)
        val expected = RankedProductsWithCount(listOf(rankedProduct), 1L)

        whenever(productRankingRepository.getRankedProductsWithCount(date, 0, 20))
            .thenReturn(expected)

        // act
        val result = service.getRankedProductsWithCount(date, 0, 20, RankingPeriod.DAILY)

        // assert
        assertThat(result).isEqualTo(expected)
    }

    // 2026-04-14 (화요일) → ISO 주차: 2026-W16 (W16 시작: 2026-04-13 월요일)
    @DisplayName("WEEKLY: mvProductRankRepository에서 yearWeek 조회")
    @Test
    fun shouldQueryMvTableForWeekly() {
        // arrange
        val date = LocalDate.of(2026, 4, 14)
        val rankedProduct = ProductRankingReadModel(productId = 1L, rank = 1L, score = 1000.0)

        whenever(mvProductRankRepository.findWeeklyRanking("2026-W16", 0, 20))
            .thenReturn(listOf(rankedProduct))
        whenever(mvProductRankRepository.countWeekly("2026-W16"))
            .thenReturn(50L)

        // act
        val result = service.getRankedProductsWithCount(date, 0, 20, RankingPeriod.WEEKLY)

        // assert
        assertThat(result.products).hasSize(1)
        assertThat(result.count).isEqualTo(50L)
    }

    @DisplayName("MONTHLY: mvProductRankRepository에서 yearMonth 조회")
    @Test
    fun shouldQueryMvTableForMonthly() {
        // arrange
        val date = LocalDate.of(2026, 4, 14)
        val rankedProduct = ProductRankingReadModel(productId = 1L, rank = 1L, score = 1000.0)

        whenever(mvProductRankRepository.findMonthlyRanking("2026-04", 0, 20))
            .thenReturn(listOf(rankedProduct))
        whenever(mvProductRankRepository.countMonthly("2026-04"))
            .thenReturn(100L)

        // act
        val result = service.getRankedProductsWithCount(date, 0, 20, RankingPeriod.MONTHLY)

        // assert
        assertThat(result.products).hasSize(1)
        assertThat(result.count).isEqualTo(100L)
    }

    @DisplayName("WEEKLY: 100번째 이후 페이지도 데이터 반환")
    @Test
    fun shouldReturnDataForWeeklyBeyondPage100() {
        // arrange
        val date = LocalDate.of(2026, 4, 14)
        val rankedProduct = ProductRankingReadModel(productId = 101L, rank = 101L, score = 500.0)

        whenever(mvProductRankRepository.findWeeklyRanking("2026-W16", 10, 20))
            .thenReturn(listOf(rankedProduct))
        whenever(mvProductRankRepository.countWeekly("2026-W16"))
            .thenReturn(200L)

        // act: page=10, size=20 → 200번째 이후 페이지
        val result = service.getRankedProductsWithCount(date, 10, 20, RankingPeriod.WEEKLY)

        // assert: TOP 100 제약 없으므로 데이터 반환
        assertThat(result.products).hasSize(1)
        assertThat(result.count).isEqualTo(200L)
    }

    @DisplayName("MONTHLY: 100번째 이후 페이지도 데이터 반환")
    @Test
    fun shouldReturnDataForMonthlyBeyondPage100() {
        // arrange
        val date = LocalDate.of(2026, 4, 14)
        val rankedProduct = ProductRankingReadModel(productId = 101L, rank = 101L, score = 400.0)

        whenever(mvProductRankRepository.findMonthlyRanking("2026-04", 5, 20))
            .thenReturn(listOf(rankedProduct))
        whenever(mvProductRankRepository.countMonthly("2026-04"))
            .thenReturn(200L)

        // act: page=5, size=20
        val result = service.getRankedProductsWithCount(date, 5, 20, RankingPeriod.MONTHLY)

        // assert: TOP 100 제약 없으므로 데이터 반환
        assertThat(result.products).hasSize(1)
        assertThat(result.count).isEqualTo(200L)
    }
}
