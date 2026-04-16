package com.loopers.interfaces.api.ranking

import com.loopers.application.api.product.ProductRankingFacade
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.product.dto.ProductInfo
import com.loopers.interfaces.api.ApiResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.LocalDate

class ProductRankingV1ControllerTest {

    private val facade: ProductRankingFacade = mock()
    private val controller = ProductRankingV1Controller(facade)

    @DisplayName("GET /api/v1/products/rankings/daily: DAILY 랭킹 조회")
    @Test
    fun shouldGetDailyRanking() {
        // arrange
        val date = LocalDate.of(2026, 4, 14)
        val productInfo = ProductInfo(
            id = 1L,
            name = "상품1",
            price = BigDecimal(10000),
            status = ProductStatus.ACTIVE,
            brandId = 1L,
            brandName = "브랜드1",
            likeCount = 0L,
            rank = 1L,
        )
        val pageData = PageImpl(listOf(productInfo), PageRequest.of(0, 20), 100L)

        whenever(facade.getRankedProducts(date, 0, 20, RankingPeriod.DAILY))
            .thenReturn(pageData)

        // act
        val response = controller.getDailyRankings(0, 20, date)

        // assert
        assertThat(response.meta.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS)
        assertThat(response.data).isNotNull()
        assertThat(response.data!!.content).hasSize(1)
        assertThat(response.data!!.totalElements).isEqualTo(100)
    }

    @DisplayName("GET /api/v1/products/rankings/weekly: WEEKLY 랭킹 조회")
    @Test
    fun shouldGetWeeklyRanking() {
        // arrange
        val date = LocalDate.of(2026, 4, 14)
        val productInfo = ProductInfo(
            id = 1L,
            name = "상품1",
            price = BigDecimal(10000),
            status = ProductStatus.ACTIVE,
            brandId = 1L,
            brandName = "브랜드1",
            likeCount = 0L,
            rank = 1L,
        )
        val pageData = PageImpl(listOf(productInfo), PageRequest.of(0, 20), 50L)

        whenever(facade.getRankedProducts(date, 0, 20, RankingPeriod.WEEKLY))
            .thenReturn(pageData)

        // act
        val response = controller.getWeeklyRankings(0, 20, date)

        // assert
        assertThat(response.meta.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS)
        assertThat(response.data!!.totalElements).isEqualTo(50)
    }

    @DisplayName("GET /api/v1/products/rankings/daily (기본값): DAILY 랭킹 조회")
    @Test
    fun shouldGetDailyRankingByDefault() {
        // arrange
        val date = LocalDate.of(2026, 4, 14)
        val productInfo = ProductInfo(
            id = 1L,
            name = "상품1",
            price = BigDecimal(10000),
            status = ProductStatus.ACTIVE,
            brandId = 1L,
            brandName = "브랜드1",
            likeCount = 0L,
            rank = 1L,
        )
        val pageData = PageImpl(listOf(productInfo), PageRequest.of(0, 20), 100L)

        whenever(facade.getRankedProducts(date, 0, 20, RankingPeriod.DAILY))
            .thenReturn(pageData)

        // act
        val response = controller.getDailyRankings(0, 20, date)

        // assert
        assertThat(response.meta.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS)
    }

    @DisplayName("페이지네이션: page=1, size=20 조회")
    @Test
    fun shouldHandlePagination() {
        // arrange
        val date = LocalDate.of(2026, 4, 14)
        val productInfo = ProductInfo(
            id = 21L,
            name = "상품21",
            price = BigDecimal(10000),
            status = ProductStatus.ACTIVE,
            brandId = 1L,
            brandName = "브랜드1",
            likeCount = 0L,
            rank = 21L,
        )
        val pageData = PageImpl(listOf(productInfo), PageRequest.of(1, 20), 100L)

        whenever(facade.getRankedProducts(date, 1, 20, RankingPeriod.DAILY))
            .thenReturn(pageData)

        // act
        val response = controller.getDailyRankings(1, 20, date)

        // assert
        assertThat(response.data!!.content).hasSize(1)
        assertThat(response.data!!.content[0].id).isEqualTo(21L)
    }
}
