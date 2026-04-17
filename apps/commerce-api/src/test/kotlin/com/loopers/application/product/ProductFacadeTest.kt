package com.loopers.application.product

import com.loopers.application.event.ProductViewActionType
import com.loopers.application.event.ProductViewedEvent
import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductSortType
import com.loopers.domain.product.ProductService
import com.loopers.domain.ranking.RankingService
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.ZonedDateTime

@DisplayName("ProductFacade")
class ProductFacadeTest {

    private val productService: ProductService = mockk()
    private val brandService: BrandService = mockk()
    private val productCacheStore: ProductCacheStore = mockk(relaxed = true)
    private val applicationEventPublisher: ApplicationEventPublisher = mockk(relaxed = true)
    private val rankingService: RankingService = mockk(relaxed = true)
    private val productFacade = ProductFacade(
        productService,
        brandService,
        productCacheStore,
        applicationEventPublisher,
        rankingService,
    )

    companion object {
        private const val PRODUCT_ID = 1L
        private const val BRAND_ID = 10L
        private const val BRAND_NAME = "루프팩"
        private const val PRODUCT_NAME = "감성 티셔츠"
        private const val PRODUCT_PRICE = 25000L
    }

    private fun createProduct(
        id: Long = PRODUCT_ID,
        name: String = PRODUCT_NAME,
        price: Long = PRODUCT_PRICE,
        brandId: Long = BRAND_ID,
        likesCount: Long = 5L,
    ): ProductModel {
        val product = ProductModel(
            name = name,
            price = price,
            brandId = brandId,
            likesCount = likesCount,
        )
        return spyk(product) {
            every { this@spyk.id } returns id
            every { createdAt } returns ZonedDateTime.now()
        }
    }

    private fun createBrand(
        id: Long = BRAND_ID,
        name: String = BRAND_NAME,
    ): BrandModel {
        val brand = BrandModel(name = name)
        return spyk(brand) {
            every { this@spyk.id } returns id
        }
    }

    @DisplayName("getProductDetail")
    @Nested
    inner class GetProductDetail {
        @DisplayName("상품과 브랜드 정보를 조합하여 ProductInfo를 반환한다")
        @Test
        fun returnsProductInfoWithBrandName_whenProductExists() {
            // arrange
            val product = createProduct()
            val brand = createBrand()
            every { productCacheStore.getProductDetail(PRODUCT_ID) } returns null
            every { productService.findById(PRODUCT_ID) } returns product
            every { brandService.findById(BRAND_ID) } returns brand

            // act
            val result = productFacade.getProductDetail(PRODUCT_ID)

            // assert
            assertThat(result.name).isEqualTo(PRODUCT_NAME)
            assertThat(result.price).isEqualTo(PRODUCT_PRICE)
            assertThat(result.brandName).isEqualTo(BRAND_NAME)
            assertThat(result.likesCount).isEqualTo(5L)
            verify(exactly = 1) { productService.findById(PRODUCT_ID) }
            verify(exactly = 1) { brandService.findById(BRAND_ID) }
            verify(exactly = 1) { productCacheStore.putProductDetail(any()) }
            verify(exactly = 1) {
                applicationEventPublisher.publishEvent(
                    match<ProductViewedEvent> {
                        it.productId == PRODUCT_ID &&
                            it.actionType == ProductViewActionType.PRODUCT_DETAIL_VIEWED
                    },
                )
            }
        }

        @DisplayName("캐시된 상품 상세가 있으면 DB 조회 없이 반환한다")
        @Test
        fun returnsCachedProductDetail_whenCacheHit() {
            // arrange
            val brand = createBrand()
            val cached = ProductCacheSnapshot(
                id = PRODUCT_ID,
                name = PRODUCT_NAME,
                price = PRODUCT_PRICE,
                brandId = BRAND_ID,
                description = null,
                thumbnailImageUrl = null,
                stockQuantity = 10,
                likesCount = 5L,
                saleStatus = com.loopers.domain.product.SaleStatus.SELLING,
                displayStatus = com.loopers.domain.product.DisplayStatus.VISIBLE,
                createdAt = ZonedDateTime.now(),
            )
            every { productCacheStore.getProductDetail(PRODUCT_ID) } returns cached
            every { brandService.findById(BRAND_ID) } returns brand

            // act
            val result = productFacade.getProductDetail(PRODUCT_ID)

            // assert
            assertThat(result.brandName).isEqualTo(BRAND_NAME)
            verify(exactly = 0) { productService.findById(any()) }
            verify(exactly = 1) { brandService.findById(BRAND_ID) }
            verify(exactly = 1) {
                applicationEventPublisher.publishEvent(
                    match<ProductViewedEvent> {
                        it.productId == PRODUCT_ID &&
                            it.actionType == ProductViewActionType.PRODUCT_DETAIL_VIEWED
                    },
                )
            }
        }
    }

    @DisplayName("getProductList")
    @Nested
    inner class GetProductList {
        @DisplayName("상품 목록 조회 시 브랜드를 일괄 조회하여 N+1을 방지한다")
        @Test
        fun returnProductInfoList_withBatchedBrandLookup() {
            // arrange
            val product1 = createProduct(id = 1L, name = "상품A", brandId = 1L)
            val product2 = createProduct(id = 2L, name = "상품B", brandId = 2L)
            val brand1 = createBrand(id = 1L, name = "브랜드A")
            val brand2 = createBrand(id = 2L, name = "브랜드B")
            val pageable = PageRequest.of(0, 10)
            val productPage = PageImpl(listOf(product1, product2), pageable, 2)

            every { productCacheStore.getProductList(null, ProductSortType.LATEST, pageable) } returns null
            every { productService.findAll(null, ProductSortType.LATEST, pageable) } returns productPage
            every { brandService.findAllByIds(listOf(1L, 2L)) } returns listOf(brand1, brand2)

            // act
            val result = productFacade.getProductList(null, ProductSortType.LATEST, pageable)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].name).isEqualTo("상품A")
            assertThat(result.content[0].brandName).isEqualTo("브랜드A")
            assertThat(result.content[1].name).isEqualTo("상품B")
            assertThat(result.content[1].brandName).isEqualTo("브랜드B")
            verify(exactly = 1) { productService.findAll(null, ProductSortType.LATEST, pageable) }
            verify(exactly = 1) { brandService.findAllByIds(any()) }
            verify(exactly = 1) { productCacheStore.putProductList(null, ProductSortType.LATEST, pageable, any()) }
            verify(exactly = 1) {
                applicationEventPublisher.publishEvent(
                    match<ProductViewedEvent> {
                        it.productId == null &&
                            it.actionType == ProductViewActionType.PRODUCT_LIST_VIEWED
                    },
                )
            }
        }

        @DisplayName("브랜드별 필터링 조회 시에도 브랜드를 일괄 조회한다")
        @Test
        fun returnsFilteredProductInfoList_whenBrandIdProvided() {
            // arrange
            val product1 = createProduct(id = 1L, name = "브랜드1 상품A", brandId = BRAND_ID)
            val product2 = createProduct(id = 2L, name = "브랜드1 상품B", brandId = BRAND_ID)
            val brand = createBrand()
            val pageable = PageRequest.of(0, 10)
            val productPage = PageImpl(listOf(product1, product2), pageable, 2)

            every { productCacheStore.getProductList(BRAND_ID, ProductSortType.LIKES_DESC, pageable) } returns null
            every { productService.findAll(BRAND_ID, ProductSortType.LIKES_DESC, pageable) } returns productPage
            every { brandService.findAllByIds(listOf(BRAND_ID)) } returns listOf(brand)

            // act
            val result = productFacade.getProductList(BRAND_ID, ProductSortType.LIKES_DESC, pageable)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.content).allSatisfy { assertThat(it.brandName).isEqualTo(BRAND_NAME) }
            verify(exactly = 1) { productService.findAll(BRAND_ID, ProductSortType.LIKES_DESC, pageable) }
            verify(exactly = 1) { brandService.findAllByIds(listOf(BRAND_ID)) }
            verify(exactly = 1) {
                applicationEventPublisher.publishEvent(
                    match<ProductViewedEvent> {
                        it.productId == null &&
                            it.actionType == ProductViewActionType.PRODUCT_LIST_VIEWED
                    },
                )
            }
        }

        @DisplayName("캐시된 상품 목록이 있으면 브랜드 일괄 조회 없이 반환한다")
        @Test
        fun returnsCachedProductList_whenCacheHit() {
            // arrange
            val pageable = PageRequest.of(0, 10)
            val brand = createBrand()
            val cachedPage = PageImpl(
                listOf(
                    ProductCacheSnapshot(
                        id = 1L,
                        name = "상품A",
                        price = 10000L,
                        brandId = BRAND_ID,
                        description = null,
                        thumbnailImageUrl = null,
                        stockQuantity = 10,
                        likesCount = 3L,
                        saleStatus = com.loopers.domain.product.SaleStatus.SELLING,
                        displayStatus = com.loopers.domain.product.DisplayStatus.VISIBLE,
                        createdAt = ZonedDateTime.now(),
                    ),
                ),
                pageable,
                1,
            )
            every { productCacheStore.getProductList(BRAND_ID, ProductSortType.PRICE_ASC, pageable) } returns cachedPage
            every { brandService.findAllByIds(listOf(BRAND_ID)) } returns listOf(brand)

            // act
            val result = productFacade.getProductList(BRAND_ID, ProductSortType.PRICE_ASC, pageable)

            // assert
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].brandName).isEqualTo(BRAND_NAME)
            verify(exactly = 0) { productService.findAll(any(), any(), any()) }
            verify(exactly = 1) { brandService.findAllByIds(listOf(BRAND_ID)) }
            verify(exactly = 1) {
                applicationEventPublisher.publishEvent(
                    match<ProductViewedEvent> {
                        it.productId == null &&
                            it.actionType == ProductViewActionType.PRODUCT_LIST_VIEWED
                    },
                )
            }
        }
    }
}
