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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@DisplayName("RankingFacade 단위 테스트")
class RankingFacadeTest {

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
    @DisplayName("랭킹 조회 시 상품 정보가 aggregation되어 반환된다")
    fun `랭킹 상품 정보 aggregation`() {
        // Arrange
        val pageResult = RankingPageResult(
            entries = listOf(
                RankedEntry(rank = 1, score = 100.0, productId = 101L),
                RankedEntry(rank = 2, score = 50.0, productId = 202L),
            ),
            totalElements = 2,
            totalPages = 1,
        )
        every { rankingService.getTopRankings("20250907", 0, 20) } returns pageResult

        every { productCacheStore.getProduct(101L) } returns null
        every { productCacheStore.getProduct(202L) } returns null
        every { productCacheStore.putProduct(any(), any()) } returns Unit

        val product101 = ProductModel(
            id = 101L, brandId = 1L, name = "상품A", description = "설명A",
            price = 10000L, stockQuantity = 10, likeCount = 5, imageUrl = "img1.jpg",
            status = ProductStatus.ACTIVE, createdAt = ZonedDateTime.now(), updatedAt = ZonedDateTime.now(),
        )
        val product202 = ProductModel(
            id = 202L, brandId = 2L, name = "상품B", description = "설명B",
            price = 20000L, stockQuantity = 0, likeCount = 3, imageUrl = "img2.jpg",
            status = ProductStatus.ACTIVE, createdAt = ZonedDateTime.now(), updatedAt = ZonedDateTime.now(),
        )
        every { productService.getProductsByIds(listOf(101L, 202L)) } returns listOf(product101, product202)

        every { brandCacheStore.getBrand(1L) } returns BrandInfo(1L, "브랜드A", "설명", "img.jpg", "ACTIVE", null, null)
        every { brandCacheStore.getBrand(2L) } returns BrandInfo(2L, "브랜드B", "설명", "img.jpg", "ACTIVE", null, null)

        // Act
        val result = rankingFacade.getRankings("20250907", 0, 20)

        // Assert
        assertThat(result.items).hasSize(2)
        assertThat(result.items[0].rank).isEqualTo(1)
        assertThat(result.items[0].product.name).isEqualTo("상품A")
        assertThat(result.items[0].product.brandName).isEqualTo("브랜드A")
        assertThat(result.items[1].rank).isEqualTo(2)
        assertThat(result.items[1].product.soldOut).isTrue()
        assertThat(result.totalElements).isEqualTo(2)
    }

    @Test
    @DisplayName("삭제된 상품은 랭킹 결과에서 제외된다")
    fun `삭제 상품 제외`() {
        // Arrange
        val pageResult = RankingPageResult(
            entries = listOf(
                RankedEntry(rank = 1, score = 100.0, productId = 101L),
                RankedEntry(rank = 2, score = 50.0, productId = 999L),
            ),
            totalElements = 2,
            totalPages = 1,
        )
        every { rankingService.getTopRankings("20250907", 0, 20) } returns pageResult
        every { productCacheStore.getProduct(101L) } returns null
        every { productCacheStore.getProduct(999L) } returns null
        every { productCacheStore.putProduct(any(), any()) } returns Unit

        val product101 = ProductModel(
            id = 101L, brandId = 1L, name = "상품A", description = "설명A",
            price = 10000L, stockQuantity = 10, likeCount = 5, imageUrl = "img1.jpg",
            status = ProductStatus.ACTIVE, createdAt = ZonedDateTime.now(), updatedAt = ZonedDateTime.now(),
        )
        // 999L은 getProductsByIds에서 반환되지 않음 (삭제됨)
        every { productService.getProductsByIds(listOf(101L, 999L)) } returns listOf(product101)
        every { brandCacheStore.getBrand(1L) } returns BrandInfo(1L, "브랜드A", "설명", "img.jpg", "ACTIVE", null, null)

        // Act
        val result = rankingFacade.getRankings("20250907", 0, 20)

        // Assert
        assertThat(result.items).hasSize(1)
        assertThat(result.items[0].product.name).isEqualTo("상품A")
    }

    @Test
    @DisplayName("빈 랭킹은 빈 결과를 반환한다")
    fun `빈 랭킹`() {
        // Arrange
        val pageResult = RankingPageResult(entries = emptyList(), totalElements = 0, totalPages = 0)
        every { rankingService.getTopRankings("20250907", 0, 20) } returns pageResult

        // Act
        val result = rankingFacade.getRankings("20250907", 0, 20)

        // Assert
        assertThat(result.items).isEmpty()
    }
}
