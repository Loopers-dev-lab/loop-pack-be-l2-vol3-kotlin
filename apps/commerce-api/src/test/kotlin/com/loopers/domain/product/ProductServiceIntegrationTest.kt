package com.loopers.domain.product

import com.loopers.domain.brand.BrandService
import com.loopers.interfaces.api.product.ProductSortType
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
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
    inner class CreateProduct {

        @Test
        fun `브랜드 생성 후 상품 생성 시 id가 0보다 크고 모든 필드가 올바르게 저장된다`() {
            val brand = createBrand()

            val product = productService.createProduct(brand.id, "Air Max", "image.png", "설명", 50_000L)

            assertAll(
                { assertThat(product.id).isGreaterThan(0) },
                { assertThat(product.brandId).isEqualTo(brand.id) },
                { assertThat(product.name.value).isEqualTo("Air Max") },
                { assertThat(product.imageUrl.value).isEqualTo("image.png") },
                { assertThat(product.description.value).isEqualTo("설명") },
                { assertThat(product.price.value).isEqualTo(50_000L) },
            )
        }
    }

    @Nested
    inner class GetProducts {

        @Test
        fun `brandId로 필터링하여 해당 브랜드의 상품 목록만 반환한다`() {
            val brand1 = createBrand()
            val brand2 = brandService.createBrand(
                name = "Adidas",
                logoImageUrl = "adidas.png",
                description = "아디다스",
                zipCode = "12345",
                roadAddress = "서울특별시 강남구 테헤란로 1",
                detailAddress = "2층",
                email = "adidas@google.com",
                phoneNumber = "02-1234-5678",
                businessNumber = "234-56-78901",
            )
            productService.createProduct(brand1.id, "Air Max", "img1.png", "설명1", 50_000L)
            productService.createProduct(brand1.id, "Air Force", "img2.png", "설명2", 30_000L)
            productService.createProduct(brand2.id, "Ultraboost", "img3.png", "설명3", 60_000L)

            val result = productService.getProducts(brand1.id, PageRequest.of(0, 10))

            assertThat(result.content).hasSize(2)
            assertThat(result.content.map { it.brandId }).containsOnly(brand1.id)
        }

        @Test
        fun `brandId 없이 전체 상품 목록을 반환한다`() {
            val brand = createBrand()
            productService.createProduct(brand.id, "Air Max", "img1.png", "설명1", 50_000L)
            productService.createProduct(brand.id, "Air Force", "img2.png", "설명2", 30_000L)

            val result = productService.getProducts(null, PageRequest.of(0, 10))

            assertThat(result.content).hasSize(2)
        }
    }

    @Nested
    inner class GetProductById {

        @Test
        fun `존재하는 ID로 상품을 조회한다`() {
            val brand = createBrand()
            val created = productService.createProduct(brand.id, "Air Max", "image.png", "설명", 50_000L)

            val result = productService.getProductById(created.id)

            assertThat(result.id).isEqualTo(created.id)
            assertThat(result.name.value).isEqualTo("Air Max")
        }

        @Test
        fun `존재하지 않는 ID로 조회 시 NOT_FOUND 예외가 발생한다`() {
            val exception = assertThrows<CoreException> {
                productService.getProductById(99999L)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class UpdateProduct {

        @Test
        fun `상품 수정 후 변경된 필드가 반영된다`() {
            val brand = createBrand()
            val product = productService.createProduct(brand.id, "Air Max", "image.png", "설명", 50_000L)

            val updated = productService.updateProduct(product.id, "Air Max 2024", "new.png", "새 설명", 60_000L)

            assertThat(updated.name.value).isEqualTo("Air Max 2024")
            assertThat(updated.imageUrl.value).isEqualTo("new.png")
            assertThat(updated.description.value).isEqualTo("새 설명")
            assertThat(updated.price.value).isEqualTo(60_000L)
        }
    }

    @Nested
    inner class DeleteProduct {

        @Test
        fun `상품 삭제 후 조회 시 NOT_FOUND 예외가 발생한다`() {
            val brand = createBrand()
            val product = productService.createProduct(brand.id, "Air Max", "image.png", "설명", 50_000L)

            productService.deleteProduct(product.id)

            val exception = assertThrows<CoreException> {
                productService.getProductById(product.id)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

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
