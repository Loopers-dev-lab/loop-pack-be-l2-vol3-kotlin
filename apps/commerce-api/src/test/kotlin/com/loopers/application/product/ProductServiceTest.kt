package com.loopers.application.product

import com.loopers.domain.product.CreateProductCommand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.UpdateProductCommand
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
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class ProductServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

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
        brandId: Long = TEST_BRAND_ID,
        name: String = TEST_NAME,
        price: BigDecimal = TEST_PRICE,
        stock: Int = TEST_STOCK,
        description: String? = TEST_DESCRIPTION,
        imageUrl: String? = TEST_IMAGE_URL,
    ): Product {
        return Product(
            brandId = brandId,
            name = name,
            price = price,
            stock = stock,
            description = description,
            imageUrl = imageUrl,
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

    @DisplayName("상품 목록을 조회할 때,")
    @Nested
    inner class GetAllProducts {

        @DisplayName("brandId 없이 조회하면, 전체 목록이 반환된다.")
        @Test
        fun returnsAllProducts_whenNoBrandIdFilter() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val products = listOf(
                createProduct(name = "에어맥스 90"),
                createProduct(name = "에어포스 1"),
            )
            val productPage = PageImpl(products, pageable, products.size.toLong())

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
            val products = listOf(createProduct(brandId = brandId))
            val productPage = PageImpl(products, pageable, products.size.toLong())

            whenever(productRepository.findAllByBrandId(brandId, pageable)).thenReturn(productPage)

            // act
            val result = productService.getAllProducts(brandId = brandId, pageable = pageable)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content[0].brandId).isEqualTo(brandId) },
            )
        }
    }

    @DisplayName("상품을 등록할 때,")
    @Nested
    inner class CreateProduct {

        @DisplayName("정상적인 정보가 주어지면, 상품이 생성된다.")
        @Test
        fun createsProduct_whenValidInfoProvided() {
            // arrange
            val command = CreateProductCommand(
                brandId = TEST_BRAND_ID,
                name = TEST_NAME,
                price = TEST_PRICE,
                stock = TEST_STOCK,
                description = TEST_DESCRIPTION,
                imageUrl = TEST_IMAGE_URL,
            )

            whenever(productRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            val result = productService.createProduct(command)

            // assert
            assertAll(
                { assertThat(result.brandId).isEqualTo(TEST_BRAND_ID) },
                { assertThat(result.name).isEqualTo(TEST_NAME) },
                { assertThat(result.price).isEqualByComparingTo(TEST_PRICE) },
                { assertThat(result.stock).isEqualTo(TEST_STOCK) },
                { assertThat(result.description).isEqualTo(TEST_DESCRIPTION) },
                { assertThat(result.imageUrl).isEqualTo(TEST_IMAGE_URL) },
            )
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
            val product = createProduct()
            val command = UpdateProductCommand(
                name = "에어포스 1",
                price = BigDecimal("139000"),
                stock = 50,
                description = "나이키 에어포스 1",
                imageUrl = "https://example.com/airforce1.jpg",
            )

            whenever(productRepository.findById(productId)).thenReturn(product)
            whenever(productRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            val result = productService.updateProduct(productId, command)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("에어포스 1") },
                { assertThat(result.price).isEqualByComparingTo(BigDecimal("139000")) },
                { assertThat(result.stock).isEqualTo(50) },
                { assertThat(result.description).isEqualTo("나이키 에어포스 1") },
                { assertThat(result.imageUrl).isEqualTo("https://example.com/airforce1.jpg") },
            )
        }

        @DisplayName("존재하지 않는 상품 ID로 수정하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsException_whenProductNotFound() {
            // arrange
            val productId = 999L
            val command = UpdateProductCommand(
                name = "에어포스 1",
                price = BigDecimal("139000"),
                stock = 50,
                description = null,
                imageUrl = null,
            )

            whenever(productRepository.findById(productId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                productService.updateProduct(productId, command)
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
            assertThat(product.isDeleted()).isTrue()
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
}
