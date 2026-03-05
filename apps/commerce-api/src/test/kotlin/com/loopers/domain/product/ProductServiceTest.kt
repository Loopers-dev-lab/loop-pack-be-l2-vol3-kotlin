package com.loopers.domain.product

import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.StockQuantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
class ProductServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    private lateinit var productService: ProductService

    @BeforeEach
    fun setUp() {
        productService = ProductService(productRepository)
    }

    private fun createProduct(
        name: String = "에어맥스",
        description: String? = "러닝화",
        price: Money = Money.of(159000L),
        stockQuantity: StockQuantity = StockQuantity.of(100),
        brandId: Long = 1L,
    ): Product = Product(
        name = name,
        description = description,
        price = price,
        likes = LikeCount.of(0),
        stockQuantity = stockQuantity,
        brandId = brandId,
    )

    @DisplayName("상품 단건 조회할 때,")
    @Nested
    inner class GetProduct {

        @DisplayName("유효한 productId를 전달하면, 상품을 반환한다.")
        @Test
        fun returnsProduct_whenValidProductIdProvided() {
            // arrange
            val productId = 1L
            val product = Product(
                name = "에어맥스",
                description = "러닝화",
                price = Money.of(159000L),
                likes = LikeCount.of(10),
                stockQuantity = StockQuantity.of(100),
                brandId = 1L,
            )

            whenever(productRepository.findById(productId)).thenReturn(product)

            // act
            val result = productService.getProduct(productId)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("에어맥스") },
                { assertThat(result.price).isEqualTo(Money.of(159000L)) },
            )
        }

        @DisplayName("존재하지 않는 productId를 전달하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductNotExists() {
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

    @DisplayName("좋아요 수를 원자적으로 증가시킬 때,")
    @Nested
    inner class IncrementLikeCount {

        @DisplayName("유효한 productId를 전달하면, Repository의 원자적 증가 메서드를 호출한다.")
        @Test
        fun callsRepositoryIncrementLikeCount_whenValidProductIdProvided() {
            // arrange
            val productId = 1L

            // act
            productService.incrementLikeCount(productId)

            // assert
            verify(productRepository).incrementLikeCount(productId)
        }
    }

    @DisplayName("좋아요 수를 원자적으로 감소시킬 때,")
    @Nested
    inner class DecrementLikeCount {

        @DisplayName("유효한 productId를 전달하면, Repository의 원자적 감소 메서드를 호출한다.")
        @Test
        fun callsRepositoryDecrementLikeCount_whenValidProductIdProvided() {
            // arrange
            val productId = 1L

            // act
            productService.decrementLikeCount(productId)

            // assert
            verify(productRepository).decrementLikeCount(productId)
        }
    }

    @DisplayName("주문용 상품을 락과 함께 조회할 때,")
    @Nested
    inner class GetProductsForOrderWithLock {

        @DisplayName("모든 상품이 존재하면, 상품 목록을 반환한다.")
        @Test
        fun returnsProducts_whenAllProductsExist() {
            // arrange
            val product1 = createProduct(name = "에어맥스").also { ReflectionTestUtils.setField(it, "id", 1L) }
            val product2 = createProduct(name = "에어포스").also { ReflectionTestUtils.setField(it, "id", 2L) }
            val productIds = listOf(1L, 2L)
            whenever(productRepository.findAllByIdsWithLock(productIds)).thenReturn(listOf(product1, product2))

            // act
            val result = productService.getProductsForOrderWithLock(productIds)

            // assert
            assertThat(result).hasSize(2)
        }

        @DisplayName("일부 상품이 존재하지 않으면, NOT_FOUND 예외를 던진다.")
        @Test
        fun throwsNotFound_whenSomeProductsNotExist() {
            // arrange
            val product1 = createProduct(name = "에어맥스").also { ReflectionTestUtils.setField(it, "id", 1L) }
            val productIds = listOf(1L, 2L)
            whenever(productRepository.findAllByIdsWithLock(productIds)).thenReturn(listOf(product1))

            // act & assert
            val exception = assertThrows<CoreException> {
                productService.getProductsForOrderWithLock(productIds)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품 생성할 때,")
    @Nested
    inner class CreateProduct {

        @DisplayName("유효한 상품 정보가 주어지면, 저장된 상품을 반환한다.")
        @Test
        fun returnsSavedProduct_whenValidInfoProvided() {
            // arrange
            val name = "에어맥스"
            val description = "러닝화"
            val price = 159000L
            val stockQuantity = 100
            val brandId = 1L
            val product = Product(
                name = name,
                description = description,
                price = Money.of(price),
                likes = LikeCount.of(0),
                stockQuantity = StockQuantity.of(stockQuantity),
                brandId = brandId,
            )

            whenever(productRepository.save(any())).thenReturn(product)

            // act
            val result = productService.createProduct(name, description, Money.of(price), StockQuantity.of(stockQuantity), brandId)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo(name) },
                { assertThat(result.description).isEqualTo(description) },
                { assertThat(result.price).isEqualTo(Money.of(price)) },
                { assertThat(result.stockQuantity).isEqualTo(StockQuantity.of(stockQuantity)) },
                { assertThat(result.brandId).isEqualTo(brandId) },
            )
        }

        @DisplayName("설명이 null이면, 설명 없이 저장된 상품을 반환한다.")
        @Test
        fun returnsSavedProduct_whenDescriptionIsNull() {
            // arrange
            val name = "에어맥스"
            val price = 159000L
            val stockQuantity = 100
            val brandId = 1L
            val product = Product(
                name = name,
                description = null,
                price = Money.of(price),
                likes = LikeCount.of(0),
                stockQuantity = StockQuantity.of(stockQuantity),
                brandId = brandId,
            )

            whenever(productRepository.save(any())).thenReturn(product)

            // act
            val result = productService.createProduct(name, null, Money.of(price), StockQuantity.of(stockQuantity), brandId)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo(name) },
                { assertThat(result.description).isNull() },
            )
        }
    }
}
