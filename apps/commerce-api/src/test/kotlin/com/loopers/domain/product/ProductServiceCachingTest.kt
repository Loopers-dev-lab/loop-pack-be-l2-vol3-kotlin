package com.loopers.domain.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ProductService 캐싱 테스트")
class ProductServiceCachingTest @Autowired constructor(
    private val productService: ProductService,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val cacheManager: CacheManager,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    private lateinit var testBrand: Brand
    private lateinit var testProduct: Product

    @BeforeEach
    fun setUp() {
        // 테스트 전 캐시 초기화
        cacheManager.getCache("product-info")?.clear()

        // 테스트 데이터 생성
        testBrand = brandRepository.save(
            Brand.create(
                name = "Test Brand ${System.nanoTime()}",
                description = "Test Description",
            ),
        )

        testProduct = productRepository.save(
            Product.create(
                brand = testBrand,
                name = "Test Product",
                price = BigDecimal("10000"),
                status = ProductStatus.ACTIVE,
            ),
        )
    }

    @AfterEach
    fun cleanup() {
        cacheManager.getCache("product-info")?.clear()
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("getProduct 첫 호출은 DB에서 조회한다")
    fun testFirstCallFetchesFromDatabase() {
        // When
        val result = productService.getProduct(testProduct.id!!)

        // Then
        assertNotNull(result)
        assertThat(result.id).isEqualTo(testProduct.id)
        assertThat(result.name).isEqualTo(testProduct.name)
        assertThat(result.status).isEqualTo(ProductStatus.ACTIVE)
    }

    @Test
    @DisplayName("getProduct 두 번째 호출은 캐시에서 반환한다")
    fun testSecondCallFetchesFromCache() {
        // Given - 첫 호출로 캐시 저장
        val firstCall = productService.getProduct(testProduct.id!!)
        assertNotNull(firstCall)

        // When - 두 번째 호출
        val secondCall = productService.getProduct(testProduct.id!!)

        // Then
        assertNotNull(secondCall)
        assertThat(secondCall.id).isEqualTo(firstCall.id)
        assertThat(secondCall.name).isEqualTo(firstCall.name)
    }

    @Test
    @DisplayName("updateProduct 후 캐시가 무효화된다")
    fun testUpdateProductInvalidatesCache() {
        // Given - 캐시에 저장
        val cachedProduct = productService.getProduct(testProduct.id!!)
        assertThat(cachedProduct.name).isEqualTo("Test Product")

        // When - 상품 수정
        productService.updateProduct(
            id = testProduct.id!!,
            name = "Updated Product",
            price = BigDecimal("20000"),
            status = ProductStatus.ACTIVE,
        )

        // Then - 캐시 무효화 후 DB에서 재조회
        val result = productService.getProduct(testProduct.id!!)
        assertThat(result.name).isEqualTo("Updated Product")
        assertThat(result.price).isEqualByComparingTo(BigDecimal("20000"))
    }

    @Test
    @DisplayName("deleteProduct 후 캐시가 무효화되고 삭제된 상품은 조회 불가")
    fun testDeleteProductInvalidatesCacheAndMarkAsDeleted() {
        // Given - 캐시에 저장
        val cachedProduct = productService.getProduct(testProduct.id!!)
        assertNotNull(cachedProduct)

        // When - 상품 삭제
        productService.deleteProduct(testProduct.id!!)

        // Then - 삭제 후 조회 시 예외 발생
        assertFailsWith<CoreException> {
            productService.getProduct(testProduct.id!!)
        }
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 예외 발생")
    fun testGetNonexistentProductThrowsException() {
        // When & Then
        val exception = assertFailsWith<CoreException> {
            productService.getProduct(999L)
        }
        assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
    }

    @Test
    @DisplayName("INACTIVE 상품은 조회할 수 없다")
    fun testGetInactiveProductThrowsException() {
        // Given - INACTIVE 상품 생성
        val inactiveProduct = productRepository.save(
            Product.create(
                brand = testBrand,
                name = "Inactive Product",
                price = BigDecimal("5000"),
                status = ProductStatus.INACTIVE,
            ),
        )

        // When & Then - INACTIVE 상품 조회 시 예외 발생
        assertFailsWith<CoreException> {
            productService.getProduct(inactiveProduct.id!!)
        }
    }

    @Test
    @DisplayName("삭제된 상품은 조회할 수 없다")
    fun testGetDeletedProductThrowsException() {
        // Given - 상품 먼저 조회 (캐시됨)
        val product = productService.getProduct(testProduct.id!!)
        assertNotNull(product)

        // When - 상품 삭제
        productService.deleteProduct(testProduct.id!!)

        // Then - 삭제된 상품 조회 시 예외 발생
        assertFailsWith<CoreException> {
            productService.getProduct(testProduct.id!!)
        }
    }

    @Test
    @DisplayName("deleteProductsByBrand 후 모든 상품이 삭제되고 캐시 무효화")
    fun testDeleteProductsByBrandInvalidatesAllProductsAndClear() {
        // Given - 여러 상품 생성 및 캐시에 저장
        val product1 = productRepository.save(
            Product.create(
                brand = testBrand,
                name = "Product 1",
                price = BigDecimal("5000"),
                status = ProductStatus.ACTIVE,
            ),
        )
        val product2 = productRepository.save(
            Product.create(
                brand = testBrand,
                name = "Product 2",
                price = BigDecimal("8000"),
                status = ProductStatus.ACTIVE,
            ),
        )

        // 캐시에 저장
        productService.getProduct(product1.id!!)
        productService.getProduct(product2.id!!)
        productService.getProduct(testProduct.id!!)

        // When - 브랜드의 모든 상품 삭제
        productService.deleteProductsByBrand(testBrand.id!!)

        // Then - 모든 상품 조회 불가
        assertAll(
            {
                assertFailsWith<CoreException> {
                    productService.getProduct(product1.id!!)
                }
            },
            {
                assertFailsWith<CoreException> {
                    productService.getProduct(product2.id!!)
                }
            },
            {
                assertFailsWith<CoreException> {
                    productService.getProduct(testProduct.id!!)
                }
            },
        )
    }

    @Test
    @DisplayName("캐시된 상품과 DB의 상품 정보는 일치한다")
    fun testCachedProductMatchesDatabaseProduct() {
        // Given
        val cachedProduct = productService.getProduct(testProduct.id!!)

        // When
        val dbProduct = productRepository.findById(testProduct.id!!)

        // Then
        assertThat(cachedProduct).isNotNull
        assertThat(dbProduct).isNotNull
        dbProduct?.let { db ->
            assertAll(
                { assertThat(cachedProduct.id).isEqualTo(db.id) },
                { assertThat(cachedProduct.name).isEqualTo(db.name) },
                { assertThat(cachedProduct.price).isEqualByComparingTo(db.price) },
                { assertThat(cachedProduct.status).isEqualTo(db.status) },
                { assertThat(cachedProduct.likeCount).isEqualTo(db.likeCount) },
            )
        }
    }

    @Test
    @DisplayName("여러 상품을 개별적으로 캐싱할 수 있다")
    fun testMultipleProductsCachedIndependently() {
        // Given - 여러 상품 생성
        val product1 = productRepository.save(
            Product.create(
                brand = testBrand,
                name = "Product 1",
                price = BigDecimal("1000"),
                status = ProductStatus.ACTIVE,
            ),
        )
        val product2 = productRepository.save(
            Product.create(
                brand = testBrand,
                name = "Product 2",
                price = BigDecimal("2000"),
                status = ProductStatus.ACTIVE,
            ),
        )

        // When - 각 상품을 개별적으로 조회
        val cached1 = productService.getProduct(product1.id!!)
        val cached2 = productService.getProduct(product2.id!!)

        // Then - 각 상품이 독립적으로 캐시됨
        assertThat(cached1.id).isEqualTo(product1.id)
        assertThat(cached2.id).isEqualTo(product2.id)
        assertThat(cached1.name).isNotEqualTo(cached2.name)
        assertThat(cached1.price).isNotEqualByComparingTo(cached2.price)
    }

    @Test
    @DisplayName("상품 정보 업데이트 후 올바른 값이 캐시된다")
    fun testUpdatedProductValuesCachedCorrectly() {
        // Given
        val originalProduct = productService.getProduct(testProduct.id!!)
        val newName = "Updated Product Name"
        val newPrice = BigDecimal("99999")

        // When - 상품을 ACTIVE 상태로 업데이트 (INACTIVE는 조회 불가)
        productService.updateProduct(
            id = testProduct.id!!,
            name = newName,
            price = newPrice,
            status = ProductStatus.ACTIVE,
        )

        // Then
        val updatedProduct = productService.getProduct(testProduct.id!!)
        assertAll(
            { assertThat(updatedProduct.name).isEqualTo(newName) },
            { assertThat(updatedProduct.price).isEqualByComparingTo(newPrice) },
            { assertThat(updatedProduct.status).isEqualTo(ProductStatus.ACTIVE) },
        )
    }

    @Test
    @DisplayName("캐시 clear 후 상품을 다시 조회할 수 있다")
    fun testProductCanBeQueriedAgainAfterCacheClear() {
        // Given
        val cachedProduct = productService.getProduct(testProduct.id!!)
        assertNotNull(cachedProduct)

        // When - 캐시 초기화
        cacheManager.getCache("product-info")?.clear()

        // Then - 다시 조회 가능
        val freshProduct = productService.getProduct(testProduct.id!!)
        assertNotNull(freshProduct)
        assertThat(freshProduct.id).isEqualTo(cachedProduct.id)
    }
}
