package com.loopers.application.ranking

import com.loopers.application.brand.BrandCacheStore
import com.loopers.application.brand.BrandInfo
import com.loopers.application.brand.BrandService
import com.loopers.application.product.ProductCacheStore
import com.loopers.application.product.ProductService
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@DisplayName("RankingFacade period 분기 테스트")
class RankingFacadePeriodTest {

    private val rankingService: RankingService = mockk()
    private val productService: ProductService = mockk()
    private val productCacheStore: ProductCacheStore = mockk()
    private val brandService: BrandService = mockk()
    private val brandCacheStore: BrandCacheStore = mockk()

    private val rankingFacade = RankingFacade(
        rankingService = rankingService,
        productService = productService,
        productCacheStore = productCacheStore,
        brandService = brandService,
        brandCacheStore = brandCacheStore,
    )

    @Test
    @DisplayName("period=WEEKLY, date=20260413 → MvRankingStore가 WEEKLY + 2026-W16 키로 호출된다")
    fun `WEEKLY period는 주간 periodKey로 조회된다`() {
        // Arrange
        val weeklyResult = RankingPageResult(
            entries = listOf(RankedEntry(rank = 1, score = 80.0, productId = 101L)),
            totalElements = 1,
            totalPages = 1,
        )
        every { rankingService.getPeriodRankings(RankingPeriod.WEEKLY, any(), 0, 20) } returns weeklyResult

        every { productCacheStore.getProduct(101L) } returns null
        every { productCacheStore.putProduct(any(), any()) } returns Unit
        val product = ProductModel(
            id = 101L, brandId = 1L, name = "상품A", description = "설명",
            price = 10000L, stockQuantity = 5, likeCount = 2, imageUrl = "img.jpg",
            status = ProductStatus.ACTIVE, createdAt = ZonedDateTime.now(), updatedAt = ZonedDateTime.now(),
        )
        every { productService.getProductsByIds(listOf(101L)) } returns listOf(product)
        every { brandCacheStore.getBrand(1L) } returns BrandInfo(1L, "브랜드A", "설명", "img.jpg", "ACTIVE", null, null)

        // Act
        val result = rankingFacade.getRankings(RankingPeriod.WEEKLY, "20260413", 0, 20)

        // Assert
        assertThat(result.items).hasSize(1)
        assertThat(result.items[0].rank).isEqualTo(1)
        verify { rankingService.getPeriodRankings(RankingPeriod.WEEKLY, any(), 0, 20) }
    }

    @Test
    @DisplayName("period=MONTHLY, date=20260413 → periodKey=202604로 조회된다")
    fun `MONTHLY period는 월간 periodKey로 조회된다`() {
        // Arrange
        val monthlyResult = RankingPageResult(
            entries = listOf(RankedEntry(rank = 1, score = 200.0, productId = 202L)),
            totalElements = 1,
            totalPages = 1,
        )
        every { rankingService.getPeriodRankings(RankingPeriod.MONTHLY, any(), 0, 20) } returns monthlyResult

        every { productCacheStore.getProduct(202L) } returns null
        every { productCacheStore.putProduct(any(), any()) } returns Unit
        val product = ProductModel(
            id = 202L, brandId = 2L, name = "상품B", description = "설명",
            price = 20000L, stockQuantity = 3, likeCount = 5, imageUrl = "img2.jpg",
            status = ProductStatus.ACTIVE, createdAt = ZonedDateTime.now(), updatedAt = ZonedDateTime.now(),
        )
        every { productService.getProductsByIds(listOf(202L)) } returns listOf(product)
        every { brandCacheStore.getBrand(2L) } returns BrandInfo(2L, "브랜드B", "설명", "img.jpg", "ACTIVE", null, null)

        // Act
        val result = rankingFacade.getRankings(RankingPeriod.MONTHLY, "20260413", 0, 20)

        // Assert
        assertThat(result.items).hasSize(1)
        assertThat(result.items[0].rank).isEqualTo(1)
        verify { rankingService.getPeriodRankings(RankingPeriod.MONTHLY, any(), 0, 20) }
    }

    @Test
    @DisplayName("period=DAILY는 기존 getTopRankings 경로를 사용한다")
    fun `DAILY period는 Redis 기반 일간 조회를 사용한다`() {
        // Arrange
        val dailyResult = RankingPageResult(
            entries = listOf(RankedEntry(rank = 1, score = 50.0, productId = 303L)),
            totalElements = 1,
            totalPages = 1,
        )
        every { rankingService.getTopRankings("20260413", 0, 20) } returns dailyResult

        every { productCacheStore.getProduct(303L) } returns null
        every { productCacheStore.putProduct(any(), any()) } returns Unit
        val product = ProductModel(
            id = 303L, brandId = 3L, name = "상품C", description = "설명",
            price = 30000L, stockQuantity = 1, likeCount = 1, imageUrl = "img3.jpg",
            status = ProductStatus.ACTIVE, createdAt = ZonedDateTime.now(), updatedAt = ZonedDateTime.now(),
        )
        every { productService.getProductsByIds(listOf(303L)) } returns listOf(product)
        every { brandCacheStore.getBrand(3L) } returns BrandInfo(3L, "브랜드C", "설명", "img.jpg", "ACTIVE", null, null)

        // Act
        val result = rankingFacade.getRankings(RankingPeriod.DAILY, "20260413", 0, 20)

        // Assert
        assertThat(result.items).hasSize(1)
        verify { rankingService.getTopRankings("20260413", 0, 20) }
        verify(exactly = 0) { rankingService.getPeriodRankings(any(), any(), any(), any()) }
    }
}
