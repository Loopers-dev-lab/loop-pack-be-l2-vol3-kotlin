package com.loopers.application.api.product

import com.loopers.domain.product.ProductStatus
import com.loopers.domain.product.dto.ProductInfo
import com.loopers.domain.ranking.ProductRankingReadService
import com.loopers.domain.ranking.ProductRankingReadModel
import com.loopers.domain.ranking.RankedProductsWithCount
import com.loopers.interfaces.api.ranking.RankingPeriod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate

class ProductRankingFacadeTest {

    private val productRankingReadService: ProductRankingReadService = mock()
    private val productFacade: ProductFacade = mock()
    private val facade = ProductRankingFacade(productFacade, productRankingReadService)

    @DisplayName("주간 랭킹: ProductRankingReadService와 ProductFacade 조합")
    @Test
    fun shouldCombineRankingAndProductInfo() {
        // arrange
        val date = LocalDate.of(2026, 4, 14)
        val rankedProduct = ProductRankingReadModel(productId = 1L, rank = 1L, score = 1000.0)
        val rankingResult = RankedProductsWithCount(listOf(rankedProduct), 50L)

        val productInfo = ProductInfo(
            id = 1L,
            name = "상품1",
            price = BigDecimal(10000),
            status = ProductStatus.ACTIVE,
            brandId = 1L,
            brandName = "브랜드1",
            likeCount = 0L,
            rank = null,
        )

        whenever(productRankingReadService.getRankedProductsWithCount(date, 0, 20, RankingPeriod.WEEKLY))
            .thenReturn(rankingResult)
        whenever(productFacade.getCachedProductInfo(1L))
            .thenReturn(productInfo)

        // act
        val result = facade.getRankedProducts(date, 0, 20, RankingPeriod.WEEKLY)

        // assert
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].id).isEqualTo(1L)
        assertThat(result.content[0].rank).isEqualTo(1L) // rank enriched
        assertThat(result.totalElements).isEqualTo(50)
    }

    @DisplayName("상품 조회 실패: 해당 상품은 제외하고 다음 상품 반환")
    @Test
    fun shouldSkipProductWhenFetchFails() {
        // arrange
        val date = LocalDate.of(2026, 4, 14)
        val rankedProducts = listOf(
            ProductRankingReadModel(productId = 1L, rank = 1L, score = 1000.0),
            ProductRankingReadModel(productId = 2L, rank = 2L, score = 900.0),
        )
        val rankingResult = RankedProductsWithCount(rankedProducts, 100L)

        val productInfo2 = ProductInfo(
            id = 2L,
            name = "상품2",
            price = BigDecimal(20000),
            status = ProductStatus.ACTIVE,
            brandId = 1L,
            brandName = "브랜드1",
            likeCount = 0L,
            rank = null,
        )

        whenever(productRankingReadService.getRankedProductsWithCount(date, 0, 20, RankingPeriod.DAILY))
            .thenReturn(rankingResult)
        whenever(productFacade.getCachedProductInfo(1L))
            .thenThrow(RuntimeException("Product not found"))
        whenever(productFacade.getCachedProductInfo(2L))
            .thenReturn(productInfo2)

        // act
        val result = facade.getRankedProducts(date, 0, 20, RankingPeriod.DAILY)

        // assert
        assertThat(result.content).hasSize(1) // 1번만 포함
        assertThat(result.content[0].id).isEqualTo(2L)
        assertThat(result.totalElements).isEqualTo(100) // count는 여전히 100
    }

    @DisplayName("페이지 정보 유지: 요청한 페이지 번호 및 크기 유지")
    @Test
    fun shouldMaintainPageInfo() {
        // arrange
        val date = LocalDate.of(2026, 4, 14)
        val rankedProduct = ProductRankingReadModel(productId = 21L, rank = 21L, score = 800.0)
        val rankingResult = RankedProductsWithCount(listOf(rankedProduct), 100L)

        val productInfo = ProductInfo(
            id = 21L,
            name = "상품21",
            price = BigDecimal(10000),
            status = ProductStatus.ACTIVE,
            brandId = 1L,
            brandName = "브랜드1",
            likeCount = 0L,
            rank = null,
        )

        whenever(productRankingReadService.getRankedProductsWithCount(date, 1, 20, RankingPeriod.DAILY))
            .thenReturn(rankingResult)
        whenever(productFacade.getCachedProductInfo(21L))
            .thenReturn(productInfo)

        // act
        val result = facade.getRankedProducts(date, 1, 20, RankingPeriod.DAILY)

        // assert
        assertThat(result.number).isEqualTo(1)
        assertThat(result.size).isEqualTo(20)
    }
}
