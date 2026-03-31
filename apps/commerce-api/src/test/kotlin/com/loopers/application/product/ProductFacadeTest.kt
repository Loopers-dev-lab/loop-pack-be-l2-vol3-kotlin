package com.loopers.application.product

import com.loopers.application.brand.BrandService
import com.loopers.application.brand.FakeBrandCacheStore
import com.loopers.application.brand.FakeBrandRepository
import com.loopers.domain.brand.BrandModel
import com.loopers.domain.product.ProductSort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProductFacade / AdminProductFacade 캐시 동작 테스트")
class ProductFacadeTest {

    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var productRepository: FakeProductRepository
    private lateinit var cacheStore: FakeProductCacheStore
    private lateinit var brandCacheStore: FakeBrandCacheStore
    private lateinit var brandService: BrandService
    private lateinit var productService: ProductService
    private lateinit var productFacade: ProductFacade
    private lateinit var adminProductFacade: AdminProductFacade
    private lateinit var productEventPublisher: com.loopers.utils.FakeEventPublisher

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        productRepository = FakeProductRepository()
        cacheStore = FakeProductCacheStore()
        brandCacheStore = FakeBrandCacheStore()
        productEventPublisher = com.loopers.utils.FakeEventPublisher()
        brandService = BrandService(brandRepository, com.loopers.utils.FakeEventPublisher())
        productService = ProductService(productRepository, productEventPublisher)
        productFacade = ProductFacade(productService, brandService, cacheStore, brandCacheStore)
        adminProductFacade = AdminProductFacade(productService, brandService)
    }

    private fun createBrand(name: String = "테스트브랜드"): BrandModel {
        return brandRepository.save(
            BrandModel(
                name = name,
                description = "브랜드 설명",
                imageUrl = "https://example.com/brand.jpg",
            ),
        )
    }

    private fun createProduct(brandId: Long, name: String = "테스트상품"): ProductInfo {
        return adminProductFacade.createProduct(
            ProductCommand.Create(
                brandId = brandId,
                name = name,
                description = "상품 설명입니다.",
                price = 10000L,
                stockQuantity = 100,
                imageUrl = "https://example.com/product.jpg",
            ),
        )
    }

    @Nested
    @DisplayName("ProductFacade.getProduct")
    inner class GetProduct {

        @Test
        @DisplayName("getProduct_cacheMiss_상품을_DB에서_조회하고_캐시에_저장한다")
        fun `cacheMiss 시 DB에서 조회 후 캐시에 저장한다`() {
            // Arrange
            val brand = createBrand()
            val product = createProduct(brand.id)
            cacheStore.clear()

            // Act
            val result = productFacade.getProduct(product.id)

            // Assert
            assertThat(result.id).isEqualTo(product.id)
            assertThat(cacheStore.getProductCallCount).isEqualTo(1)
            assertThat(cacheStore.putProductCallCount).isEqualTo(1)
        }

        @Test
        @DisplayName("getProduct_cacheHit_캐시에서_바로_반환한다")
        fun `cacheHit 시 캐시에서 바로 반환한다`() {
            // Arrange
            val brand = createBrand()
            val product = createProduct(brand.id)
            val cachedInfo = ProductInfo(
                id = product.id,
                brandId = brand.id,
                brandName = "캐시된브랜드",
                name = "캐시된상품",
                description = "캐시된설명",
                price = 99999L,
                stockQuantity = 1,
                likeCount = 0,
                imageUrl = "https://example.com/cached.jpg",
                status = "ACTIVE",
                createdAt = null,
                updatedAt = null,
            )
            cacheStore.clear()
            cacheStore.seedProduct(product.id, cachedInfo)

            // Act
            val result = productFacade.getProduct(product.id)

            // Assert
            assertThat(result.name).isEqualTo("캐시된상품")
            assertThat(result.price).isEqualTo(99999L)
            assertThat(cacheStore.getProductCallCount).isEqualTo(1)
            assertThat(cacheStore.putProductCallCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("ProductFacade.getProducts")
    inner class GetProducts {

        @Test
        @DisplayName("getProducts_cacheMiss_목록을_DB에서_조회하고_캐시에_저장한다")
        fun `cacheMiss 시 DB에서 목록을 조회 후 캐시에 저장한다`() {
            // Arrange
            val brand = createBrand()
            createProduct(brand.id, "상품A")
            createProduct(brand.id, "상품B")
            cacheStore.clear()

            // Act
            val result = productFacade.getProducts(
                brandId = null,
                sort = ProductSort.LATEST,
                size = 10,
                cursor = null,
            )

            // Assert
            assertThat(result.data).hasSize(2)
            assertThat(cacheStore.getProductListCallCount).isEqualTo(1)
            assertThat(cacheStore.putProductListCallCount).isEqualTo(1)
        }

        @Test
        @DisplayName("getProducts_cacheHit_캐시에서_바로_반환한다")
        fun `cacheHit 시 캐시에서 목록을 바로 반환한다`() {
            // Arrange
            val brand = createBrand()
            createProduct(brand.id, "실제상품")
            cacheStore.clear()

            val cachedResult = ProductListResult(
                data = listOf(
                    ProductInfo(
                        id = 999L,
                        brandId = brand.id,
                        brandName = "캐시브랜드",
                        name = "캐시상품",
                        description = "캐시설명",
                        price = 5000L,
                        stockQuantity = 10,
                        likeCount = 0,
                        imageUrl = "https://example.com/cache.jpg",
                        status = "ACTIVE",
                        createdAt = null,
                        updatedAt = null,
                    ),
                ),
                nextCursor = null,
                hasNext = false,
            )
            // 캐시 키: "all:LATEST:10:first"
            cacheStore.seedProductList("all:LATEST:10:first", cachedResult)

            // Act
            val result = productFacade.getProducts(
                brandId = null,
                sort = ProductSort.LATEST,
                size = 10,
                cursor = null,
            )

            // Assert
            assertThat(result.data).hasSize(1)
            assertThat(result.data[0].name).isEqualTo("캐시상품")
            assertThat(cacheStore.getProductListCallCount).isEqualTo(1)
            assertThat(cacheStore.putProductListCallCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("AdminProductFacade 캐시 무효화")
    inner class AdminCacheEviction {

        @Test
        @DisplayName("updateProduct_수정_시_ProductUpdatedEvent가_발행된다")
        fun `updateProduct 수정 시 ProductUpdatedEvent가 발행된다`() {
            // Arrange
            val brand = createBrand()
            val product = createProduct(brand.id)
            productEventPublisher.clear()

            // Act
            adminProductFacade.updateProduct(
                id = product.id,
                command = ProductCommand.Update(
                    name = "수정된상품",
                    description = "수정된설명입니다.",
                    price = 20000L,
                    stockQuantity = 50,
                    imageUrl = "https://example.com/updated.jpg",
                ),
            )

            // Assert
            assertThat(productEventPublisher.hasEvent<com.loopers.domain.common.event.ProductUpdatedEvent>()).isTrue()
        }

        @Test
        @DisplayName("deleteProduct_삭제_시_ProductDeletedEvent가_발행된다")
        fun `deleteProduct 삭제 시 ProductDeletedEvent가 발행된다`() {
            // Arrange
            val brand = createBrand()
            val product = createProduct(brand.id)
            productEventPublisher.clear()

            // Act
            adminProductFacade.deleteProduct(product.id)

            // Assert
            assertThat(productEventPublisher.hasEvent<com.loopers.domain.common.event.ProductDeletedEvent>()).isTrue()
        }
    }
}
