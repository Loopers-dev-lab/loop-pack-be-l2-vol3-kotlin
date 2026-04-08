package com.loopers.application.ranking

import com.loopers.application.product.ProductCacheManager
import com.loopers.application.product.ProductDetailInfo
import com.loopers.domain.ranking.RankingEntry
import com.loopers.domain.ranking.RankingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("RankingFacade")
class RankingFacadeTest {

    @Mock
    private lateinit var rankingService: RankingService

    @Mock
    private lateinit var productCacheManager: ProductCacheManager

    @InjectMocks
    private lateinit var rankingFacade: RankingFacade

    private val today = LocalDate.of(2026, 4, 9)

    private fun createProductDetailInfo(id: Long, name: String, price: Long) = ProductDetailInfo(
        id = id,
        name = name,
        price = price,
        description = null,
        brandId = 1L,
        brandName = "테스트 브랜드",
        likeCount = 0,
    )

    @DisplayName("랭킹 목록 조회 시,")
    @Nested
    inner class GetRankings {

        @DisplayName("상품 정보가 함께 반환된다.")
        @Test
        fun returnsRankingsWithProductInfo() {
            // arrange
            val entries = listOf(
                RankingEntry(100L, 80.0),
                RankingEntry(200L, 50.0),
            )
            whenever(rankingService.getTopRankings(today, 1, 20)).thenReturn(entries)
            whenever(productCacheManager.getProduct(100L))
                .thenReturn(createProductDetailInfo(100L, "상품A", 10000L))
            whenever(productCacheManager.getProduct(200L))
                .thenReturn(createProductDetailInfo(200L, "상품B", 20000L))

            // act
            val result = rankingFacade.getRankings(today, page = 1, size = 20)

            // assert
            assertThat(result).hasSize(2)
            assertThat(result[0].productId).isEqualTo(100L)
            assertThat(result[0].productName).isEqualTo("상품A")
            assertThat(result[0].rank).isEqualTo(1L)
            assertThat(result[0].score).isEqualTo(80.0)
        }

        @DisplayName("ZSET 순서대로 상품 정보가 매핑된다.")
        @Test
        fun preservesZsetOrder() {
            // arrange
            val entries = listOf(
                RankingEntry(300L, 90.0),
                RankingEntry(100L, 70.0),
                RankingEntry(200L, 50.0),
            )
            whenever(rankingService.getTopRankings(today, 1, 20)).thenReturn(entries)
            whenever(productCacheManager.getProduct(300L))
                .thenReturn(createProductDetailInfo(300L, "상품C", 30000L))
            whenever(productCacheManager.getProduct(100L))
                .thenReturn(createProductDetailInfo(100L, "상품A", 10000L))
            whenever(productCacheManager.getProduct(200L))
                .thenReturn(createProductDetailInfo(200L, "상품B", 20000L))

            // act
            val result = rankingFacade.getRankings(today, page = 1, size = 20)

            // assert
            assertThat(result).hasSize(3)
            assertThat(result[0].productId).isEqualTo(300L)
            assertThat(result[0].rank).isEqualTo(1L)
            assertThat(result[1].productId).isEqualTo(100L)
            assertThat(result[1].rank).isEqualTo(2L)
            assertThat(result[2].productId).isEqualTo(200L)
            assertThat(result[2].rank).isEqualTo(3L)
        }
    }
}
