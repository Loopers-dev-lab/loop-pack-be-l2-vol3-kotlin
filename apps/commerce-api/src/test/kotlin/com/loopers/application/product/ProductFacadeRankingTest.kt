package com.loopers.application.product

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductService
import com.loopers.domain.ranking.RankingService
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.time.ZonedDateTime

@DisplayName("ProductFacade - 상품 상세 랭킹 정보")
class ProductFacadeRankingTest {

    private val productService: ProductService = mockk()
    private val brandService: BrandService = mockk()
    private val productCacheStore: ProductCacheStore = mockk(relaxed = true)
    private val applicationEventPublisher: ApplicationEventPublisher = mockk(relaxed = true)
    private val rankingService: RankingService = mockk()
    private val productFacade = ProductFacade(
        productService,
        brandService,
        productCacheStore,
        applicationEventPublisher,
        rankingService,
    )

    @DisplayName("상품 상세 조회 시 오늘 기준 순위가 포함된다")
    @Test
    fun includesRankingInProductDetail() {
        // arrange
        val product = createProduct(id = 1L)
        val brand = createBrand()
        every { productCacheStore.getProductDetail(1L) } returns null
        every { productService.findById(1L) } returns product
        every { brandService.findById(10L) } returns brand
        every { rankingService.getProductRank(LocalDate.now(), 1L) } returns 3L

        // act
        val result = productFacade.getProductDetail(1L)

        // assert
        assertThat(result.ranking).isEqualTo(3)
    }

    @DisplayName("순위에 없는 상품이면 ranking이 null이다")
    @Test
    fun rankingIsNullWhenNotRanked() {
        val product = createProduct(id = 2L)
        val brand = createBrand()
        every { productCacheStore.getProductDetail(2L) } returns null
        every { productService.findById(2L) } returns product
        every { brandService.findById(10L) } returns brand
        every { rankingService.getProductRank(LocalDate.now(), 2L) } returns null

        val result = productFacade.getProductDetail(2L)

        assertThat(result.ranking).isNull()
    }

    @DisplayName("캐시 히트 시에도 순위 정보가 포함된다")
    @Test
    fun includesRankingEvenOnCacheHit() {
        val brand = createBrand()
        val cached = ProductCacheSnapshot(
            id = 1L,
            name = "상품",
            price = 1000L,
            brandId = 10L,
            description = null,
            thumbnailImageUrl = null,
            stockQuantity = 10,
            likesCount = 5L,
            saleStatus = com.loopers.domain.product.SaleStatus.SELLING,
            displayStatus = com.loopers.domain.product.DisplayStatus.VISIBLE,
            createdAt = ZonedDateTime.now(),
        )
        every { productCacheStore.getProductDetail(1L) } returns cached
        every { brandService.findById(10L) } returns brand
        every { rankingService.getProductRank(LocalDate.now(), 1L) } returns 1L

        val result = productFacade.getProductDetail(1L)

        assertThat(result.ranking).isEqualTo(1)
    }

    private fun createProduct(id: Long): ProductModel {
        val product = ProductModel(name = "상품", price = 1000L, brandId = 10L, stockQuantity = 10)
        return spyk(product) {
            every { this@spyk.id } returns id
            every { createdAt } returns ZonedDateTime.now()
        }
    }

    private fun createBrand(): BrandModel {
        val brand = BrandModel(name = "루프팩")
        return spyk(brand) { every { this@spyk.id } returns 10L }
    }
}
