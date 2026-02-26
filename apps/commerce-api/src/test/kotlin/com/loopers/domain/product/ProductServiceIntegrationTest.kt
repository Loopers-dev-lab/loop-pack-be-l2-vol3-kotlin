package com.loopers.domain.product

import com.loopers.domain.brand.BrandService
import com.loopers.interfaces.api.product.ProductSortType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest

@SpringBootTest
class ProductServiceIntegrationTest @Autowired constructor(
    private val brandService: BrandService,
    private val productService: ProductService,
    private val productRepository: ProductRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand() = brandService.createBrand(
        name = "Nike",
        logoImageUrl = "test.png",
        description = "테스트 브랜드",
        zipCode = "12345",
        roadAddress = "서울특별시 중구 테스트길 1",
        detailAddress = "1층",
        email = "nike@google.com",
        phoneNumber = "02-3783-4401",
        businessNumber = "123-45-67890",
    )

    @Nested
    inner class SortProducts {

        private var productAId: Long = 0L
        private var productBId: Long = 0L
        private var productCId: Long = 0L

        @BeforeEach
        fun setUp() {
            val brand = createBrand()

            // 상품 C: price=5000, likeCount=1 (가장 먼저 생성)
            val productC = productService.createProduct(brand.id, "ProductC", "c.png", "설명C", 5_000L)
            repeat(1) { productRepository.save(productC.also { it.increaseLikeCount() }) }
            productCId = productC.id

            // 상품 B: price=10000, likeCount=10 (중간 생성)
            val productB = productService.createProduct(brand.id, "ProductB", "b.png", "설명B", 10_000L)
            repeat(10) { productRepository.save(productB.also { it.increaseLikeCount() }) }
            productBId = productB.id

            // 상품 A: price=30000, likeCount=5 (가장 나중 생성)
            val productA = productService.createProduct(brand.id, "ProductA", "a.png", "설명A", 30_000L)
            repeat(5) { productRepository.save(productA.also { it.increaseLikeCount() }) }
            productAId = productA.id
        }

        @Test
        fun `PRICE_ASC 정렬 시 가격 오름차순으로 반환된다`() {
            val page = productService.getProducts(
                brandId = null,
                pageable = PageRequest.of(0, 10, ProductSortType.PRICE_ASC.sort),
            )

            assertThat(page.content.map { it.price.value })
                .containsExactly(5_000L, 10_000L, 30_000L)
        }

        @Test
        fun `LIKES_DESC 정렬 시 좋아요 수 내림차순으로 반환된다`() {
            val page = productService.getProducts(
                brandId = null,
                pageable = PageRequest.of(0, 10, ProductSortType.LIKES_DESC.sort),
            )

            assertThat(page.content.map { it.likeCount.value })
                .containsExactly(10L, 5L, 1L)
        }

        @Test
        fun `LATEST 정렬 시 최신 생성 순으로 반환된다`() {
            val page = productService.getProducts(
                brandId = null,
                pageable = PageRequest.of(0, 10, ProductSortType.LATEST.sort),
            )

            assertThat(page.content.map { it.price.value })
                .containsExactly(30_000L, 10_000L, 5_000L)
        }
    }
}
