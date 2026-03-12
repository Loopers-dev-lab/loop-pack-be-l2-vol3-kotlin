package com.loopers.application.product

import com.loopers.application.order.OrderItemCriteria
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.infrastructure.product.ProductCacheService
import com.loopers.support.cache.CachedPage
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class ProductServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var brandRepository: BrandRepository

    @Mock
    private lateinit var productCacheService: ProductCacheService

    @InjectMocks
    private lateinit var productService: ProductService

    companion object {
        private const val TEST_BRAND_ID = 1L
        private const val TEST_NAME = "에어맥스 90"
        private val TEST_PRICE = BigDecimal("129000")
        private const val TEST_STOCK = 100
        private const val TEST_DESCRIPTION = "나이키 에어맥스 90"
        private const val TEST_IMAGE_URL = "https://example.com/airmax90.jpg"
    }

    private fun createProduct(
        id: Long = 0L,
        brandId: Long = TEST_BRAND_ID,
        name: String = TEST_NAME,
        price: BigDecimal = TEST_PRICE,
        stock: Int = TEST_STOCK,
        description: String? = TEST_DESCRIPTION,
        imageUrl: String? = TEST_IMAGE_URL,
    ): Product {
        val product = Product(
            brandId = brandId,
            name = name,
            price = price,
            stock = stock,
            description = description,
            imageUrl = imageUrl,
        )
        if (id > 0) {
            ReflectionTestUtils.setField(product, "id", id)
        }
        return product
    }

    private fun createProductInfo(
        id: Long = 1L,
        brandId: Long = TEST_BRAND_ID,
        name: String = TEST_NAME,
        price: BigDecimal = TEST_PRICE,
        stock: Int = TEST_STOCK,
        likeCount: Int = 0,
    ): ProductInfo {
        val now = ZonedDateTime.now()
        return ProductInfo(
            id = id,
            brandId = brandId,
            name = name,
            price = price,
            stock = stock,
            likeCount = likeCount,
            description = TEST_DESCRIPTION,
            imageUrl = TEST_IMAGE_URL,
            createdAt = now,
            updatedAt = now,
        )
    }

    @DisplayName("상품을 조회할 때,")
    @Nested
    inner class GetProduct {

        @DisplayName("존재하는 상품 ID로 조회하면, 상품 정보가 반환된다.")
        @Test
        fun returnsProduct_whenProductExists() {
            // arrange
            val productId = 1L
            val product = createProduct()

            whenever(productRepository.findById(productId)).thenReturn(product)

            // act
            val result = productService.getProduct(productId)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo(TEST_NAME) },
                { assertThat(result.price).isEqualByComparingTo(TEST_PRICE) },
                { assertThat(result.stock).isEqualTo(TEST_STOCK) },
            )
        }

        @DisplayName("존재하지 않는 상품 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsException_whenProductNotFound() {
            // arrange
            val productId = 999L

            whenever(productRepository.findById(productId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                productService.getProduct(productId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품 상세를 조회할 때,")
    @Nested
    inner class GetProductInfo {

        @DisplayName("캐시에 데이터가 있으면, Repository를 호출하지 않고 캐시 데이터를 반환한다.")
        @Test
        fun returnsCachedData_whenCacheHit() {
            // arrange
            val productId = 1L
            val cachedInfo = createProductInfo(id = productId)

            whenever(productCacheService.getProductDetail(productId)).thenReturn(cachedInfo)

            // act
            val result = productService.getProductInfo(productId)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(productId) },
                { assertThat(result.name).isEqualTo(TEST_NAME) },
                { verify(productRepository, never()).findById(any()) },
            )
        }

        @DisplayName("캐시에 데이터가 없으면, Repository를 호출하고 캐시에 저장한다.")
        @Test
        fun fetchesFromDbAndCaches_whenCacheMiss() {
            // arrange
            val productId = 1L
            val now = ZonedDateTime.now()
            val product = createProduct(id = productId)
            ReflectionTestUtils.setField(product, "createdAt", now)
            ReflectionTestUtils.setField(product, "updatedAt", now)

            whenever(productCacheService.getProductDetail(productId)).thenReturn(null)
            whenever(productRepository.findById(productId)).thenReturn(product)

            // act
            val result = productService.getProductInfo(productId)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo(TEST_NAME) },
                { verify(productRepository).findById(productId) },
                { verify(productCacheService).setProductDetail(eq(productId), any()) },
            )
        }
    }

    @DisplayName("상품 목록을 조회할 때,")
    @Nested
    inner class GetAllProducts {

        @DisplayName("brandId 없이 조회하면, 전체 목록이 반환된다.")
        @Test
        fun returnsAllProducts_whenNoBrandIdFilter() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val now = ZonedDateTime.now()
            val product1 = createProduct(name = "에어맥스 90")
            ReflectionTestUtils.setField(product1, "createdAt", now)
            ReflectionTestUtils.setField(product1, "updatedAt", now)
            val product2 = createProduct(name = "에어포스 1")
            ReflectionTestUtils.setField(product2, "createdAt", now)
            ReflectionTestUtils.setField(product2, "updatedAt", now)
            val products = listOf(product1, product2)
            val productPage = PageImpl(products, pageable, products.size.toLong())

            whenever(productCacheService.getProductList(anyOrNull(), any(), any(), any())).thenReturn(null)
            whenever(productRepository.findAll(pageable)).thenReturn(productPage)

            // act
            val result = productService.getAllProducts(brandId = null, pageable = pageable)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.content[0].name).isEqualTo("에어맥스 90") },
                { assertThat(result.content[1].name).isEqualTo("에어포스 1") },
            )
        }

        @DisplayName("brandId로 필터링하면, 해당 브랜드 상품만 반환된다.")
        @Test
        fun returnsFilteredProducts_whenBrandIdProvided() {
            // arrange
            val brandId = 1L
            val pageable = PageRequest.of(0, 20)
            val now = ZonedDateTime.now()
            val product = createProduct(brandId = brandId)
            ReflectionTestUtils.setField(product, "createdAt", now)
            ReflectionTestUtils.setField(product, "updatedAt", now)
            val products = listOf(product)
            val productPage = PageImpl(products, pageable, products.size.toLong())

            whenever(productCacheService.getProductList(anyOrNull(), any(), any(), any())).thenReturn(null)
            whenever(productRepository.findAllByBrandId(brandId, pageable)).thenReturn(productPage)

            // act
            val result = productService.getAllProducts(brandId = brandId, pageable = pageable)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content[0].brandId).isEqualTo(brandId) },
            )
        }

        @DisplayName("Sort가 적용된 Pageable이 Repository에 전달된다.")
        @Test
        fun passesPageableWithSort_whenSortProvided() {
            // arrange
            val sort = Sort.by(Sort.Direction.ASC, "price")
            val pageable = PageRequest.of(0, 20, sort)
            val now = ZonedDateTime.now()
            val product = createProduct()
            ReflectionTestUtils.setField(product, "createdAt", now)
            ReflectionTestUtils.setField(product, "updatedAt", now)
            val productPage = PageImpl(listOf(product), pageable, 1L)

            whenever(productCacheService.getProductList(anyOrNull(), any(), any(), any())).thenReturn(null)
            whenever(productRepository.findAll(pageable)).thenReturn(productPage)

            // act
            productService.getAllProducts(brandId = null, pageable = pageable)

            // assert
            verify(productRepository).findAll(pageable)
        }

        @DisplayName("캐시에 데이터가 있으면, Repository를 호출하지 않고 캐시 데이터를 반환한다.")
        @Test
        fun returnsCachedData_whenCacheHit() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val cachedPage = CachedPage(
                content = listOf(createProductInfo(id = 1L), createProductInfo(id = 2L)),
                page = 0,
                size = 20,
                totalElements = 2L,
            )

            whenever(productCacheService.getProductList(anyOrNull(), any(), any(), any())).thenReturn(cachedPage)

            // act
            val result = productService.getAllProducts(brandId = null, pageable = pageable)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { verify(productRepository, never()).findAll(any<org.springframework.data.domain.Pageable>()) },
            )
        }

        @DisplayName("캐시에 데이터가 없으면, Repository를 호출하고 캐시에 저장한다.")
        @Test
        fun fetchesFromDbAndCaches_whenCacheMiss() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val now = ZonedDateTime.now()
            val product = createProduct()
            ReflectionTestUtils.setField(product, "createdAt", now)
            ReflectionTestUtils.setField(product, "updatedAt", now)
            val productPage = PageImpl(listOf(product), pageable, 1L)

            whenever(productCacheService.getProductList(anyOrNull(), any(), any(), any())).thenReturn(null)
            whenever(productRepository.findAll(pageable)).thenReturn(productPage)

            // act
            productService.getAllProducts(brandId = null, pageable = pageable)

            // assert
            verify(productRepository).findAll(pageable)
            verify(productCacheService).setProductList(anyOrNull(), any(), any(), any(), any())
        }
    }

    @DisplayName("상품을 등록할 때,")
    @Nested
    inner class CreateProduct {

        @DisplayName("브랜드가 존재하면, 상품이 생성된다.")
        @Test
        fun createsProduct_whenBrandExists() {
            // arrange
            val now = ZonedDateTime.now()
            val criteria = CreateProductCriteria(
                brandId = TEST_BRAND_ID,
                name = TEST_NAME,
                price = TEST_PRICE,
                stock = TEST_STOCK,
                description = TEST_DESCRIPTION,
                imageUrl = TEST_IMAGE_URL,
            )

            whenever(brandRepository.existsById(TEST_BRAND_ID)).thenReturn(true)
            whenever(productRepository.save(any())).thenAnswer {
                val product = it.arguments[0] as Product
                ReflectionTestUtils.setField(product, "createdAt", now)
                ReflectionTestUtils.setField(product, "updatedAt", now)
                product
            }

            // act
            val result = productService.createProduct(criteria)

            // assert
            assertAll(
                { assertThat(result.brandId).isEqualTo(TEST_BRAND_ID) },
                { assertThat(result.name).isEqualTo(TEST_NAME) },
                { assertThat(result.price).isEqualByComparingTo(TEST_PRICE) },
                { assertThat(result.stock).isEqualTo(TEST_STOCK) },
                { assertThat(result.description).isEqualTo(TEST_DESCRIPTION) },
                { assertThat(result.imageUrl).isEqualTo(TEST_IMAGE_URL) },
                { verify(productCacheService).evictAllProductLists() },
            )
        }

        @DisplayName("브랜드가 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsException_whenBrandNotFound() {
            // arrange
            val criteria = CreateProductCriteria(
                brandId = 999L,
                name = TEST_NAME,
                price = TEST_PRICE,
                stock = TEST_STOCK,
                description = null,
                imageUrl = null,
            )

            whenever(brandRepository.existsById(999L)).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                productService.createProduct(criteria)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품을 수정할 때,")
    @Nested
    inner class UpdateProduct {

        @DisplayName("정상적인 정보가 주어지면, 상품이 수정된다.")
        @Test
        fun updatesProduct_whenValidInfoProvided() {
            // arrange
            val productId = 1L
            val now = ZonedDateTime.now()
            val product = createProduct()
            ReflectionTestUtils.setField(product, "createdAt", now)
            ReflectionTestUtils.setField(product, "updatedAt", now)
            val criteria = UpdateProductCriteria(
                name = "에어포스 1",
                price = BigDecimal("139000"),
                stock = 50,
                description = "나이키 에어포스 1",
                imageUrl = "https://example.com/airforce1.jpg",
            )

            whenever(productRepository.findById(productId)).thenReturn(product)
            whenever(productRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            val result = productService.updateProduct(productId, criteria)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("에어포스 1") },
                { assertThat(result.price).isEqualByComparingTo(BigDecimal("139000")) },
                { assertThat(result.stock).isEqualTo(50) },
                { assertThat(result.description).isEqualTo("나이키 에어포스 1") },
                { assertThat(result.imageUrl).isEqualTo("https://example.com/airforce1.jpg") },
                { verify(productCacheService).evictProductDetail(productId) },
                { verify(productCacheService).evictAllProductLists() },
            )
        }

        @DisplayName("존재하지 않는 상품 ID로 수정하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsException_whenProductNotFound() {
            // arrange
            val productId = 999L
            val criteria = UpdateProductCriteria(
                name = "에어포스 1",
                price = BigDecimal("139000"),
                stock = 50,
                description = null,
                imageUrl = null,
            )

            whenever(productRepository.findById(productId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                productService.updateProduct(productId, criteria)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품을 삭제할 때,")
    @Nested
    inner class DeleteProduct {

        @DisplayName("존재하는 상품을 삭제하면, soft delete 된다.")
        @Test
        fun deletesProduct_whenProductExists() {
            // arrange
            val productId = 1L
            val product = createProduct()

            whenever(productRepository.findById(productId)).thenReturn(product)
            whenever(productRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            productService.deleteProduct(productId)

            // assert
            assertAll(
                { assertThat(product.isDeleted()).isTrue() },
                { verify(productCacheService).evictProductDetail(productId) },
                { verify(productCacheService).evictAllProductLists() },
            )
        }

        @DisplayName("존재하지 않는 상품을 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsException_whenProductNotFound() {
            // arrange
            val productId = 999L

            whenever(productRepository.findById(productId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                productService.deleteProduct(productId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("좋아요 수를 증가시킬 때,")
    @Nested
    inner class IncrementLikeCount {

        @DisplayName("productId로 호출하면, Repository의 incrementLikeCount가 호출된다.")
        @Test
        fun callsRepositoryIncrementLikeCount() {
            // arrange
            val productId = 1L

            // act
            productService.incrementLikeCount(productId)

            // assert
            assertAll(
                { verify(productRepository).incrementLikeCount(productId) },
                { verify(productCacheService).evictProductDetail(productId) },
                { verify(productCacheService).evictAllProductLists() },
            )
        }
    }

    @DisplayName("좋아요 수를 감소시킬 때,")
    @Nested
    inner class DecrementLikeCount {

        @DisplayName("productId로 호출하면, Repository의 decrementLikeCount가 호출된다.")
        @Test
        fun callsRepositoryDecrementLikeCount() {
            // arrange
            val productId = 1L

            // act
            productService.decrementLikeCount(productId)

            // assert
            assertAll(
                { verify(productRepository).decrementLikeCount(productId) },
                { verify(productCacheService).evictProductDetail(productId) },
                { verify(productCacheService).evictAllProductLists() },
            )
        }
    }

    @DisplayName("재고를 예약할 때,")
    @Nested
    inner class ReserveStock {

        @DisplayName("모든 상품의 재고가 충분하면, 전부 예약 성공한다.")
        @Test
        fun reservesAllProducts_whenAllStockSufficient() {
            // arrange
            val product1 = createProduct(id = 1L, name = "에어맥스 90", stock = 100, price = BigDecimal("129000"))
            val product2 = createProduct(id = 2L, brandId = 2L, name = "울트라부스트", stock = 50, price = BigDecimal("199000"))
            val products = listOf(product1, product2)
            val criteria = listOf(
                OrderItemCriteria(productId = 1L, quantity = 2),
                OrderItemCriteria(productId = 2L, quantity = 1),
            )

            // act
            val result = productService.reserveStock(products, criteria)

            // assert
            assertAll(
                { assertThat(result).hasSize(2) },
                { assertThat(product1.stock).isEqualTo(98) },
                { assertThat(product2.stock).isEqualTo(49) },
            )
        }

        @DisplayName("일부 상품의 재고가 부족하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenSomeStockInsufficient() {
            // arrange
            val product1 = createProduct(id = 1L, name = "에어맥스 90", stock = 100, price = BigDecimal("129000"))
            val product2 = createProduct(id = 2L, brandId = 2L, name = "울트라부스트", stock = 0, price = BigDecimal("199000"))
            val products = listOf(product1, product2)
            val criteria = listOf(
                OrderItemCriteria(productId = 1L, quantity = 2),
                OrderItemCriteria(productId = 2L, quantity = 1),
            )

            // act
            val exception = assertThrows<CoreException> {
                productService.reserveStock(products, criteria)
            }

            // assert
            assertAll(
                { assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST) },
                { assertThat(exception.message).contains("재고가 부족한 상품이 있습니다") },
                { assertThat(product1.stock).isEqualTo(100) },
            )
        }

        @DisplayName("상품이 존재하지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenProductNotFound() {
            // arrange
            val product1 = createProduct(id = 1L, name = "에어맥스 90", stock = 100, price = BigDecimal("129000"))
            val products = listOf(product1)
            val criteria = listOf(
                OrderItemCriteria(productId = 1L, quantity = 2),
                OrderItemCriteria(productId = 999L, quantity = 1),
            )

            // act
            val exception = assertThrows<CoreException> {
                productService.reserveStock(products, criteria)
            }

            // assert
            assertAll(
                { assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST) },
                { assertThat(exception.message).contains("존재하지 않는 상품") },
                { assertThat(product1.stock).isEqualTo(100) },
            )
        }
    }
}
