package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

@DisplayName("ProductService")
class ProductServiceTest {

    private val productRepository: ProductRepository = mockk()
    private val productService = ProductService(productRepository)

    companion object {
        private const val VALID_NAME = "감성 티셔츠"
        private const val VALID_PRICE = 25000L
        private const val VALID_BRAND_ID = 1L
        private const val VALID_DESCRIPTION = "부드러운 면 소재의 감성 티셔츠"
        private const val VALID_THUMBNAIL_URL = "https://example.com/product.png"
    }

    @DisplayName("findById")
    @Nested
    inner class FindById {
        @DisplayName("존재하는 상품 ID면 상품이 반환된다")
        @Test
        fun returnsProduct_whenProductExists() {
            // arrange
            val product = ProductModel(
                name = VALID_NAME,
                price = VALID_PRICE,
                brandId = VALID_BRAND_ID,
                description = VALID_DESCRIPTION,
                thumbnailImageUrl = VALID_THUMBNAIL_URL,
            )
            every { productRepository.findByIdAndDeletedAtIsNull(1L) } returns product

            // act
            val result = productService.findById(1L)

            // assert
            assertThat(result.name).isEqualTo(VALID_NAME)
            assertThat(result.price).isEqualTo(VALID_PRICE)
            assertThat(result.brandId).isEqualTo(VALID_BRAND_ID)
            verify(exactly = 1) { productRepository.findByIdAndDeletedAtIsNull(1L) }
        }

        @DisplayName("존재하지 않는 상품 ID면 NOT_FOUND 예외가 발생한다")
        @Test
        fun throwsNotFoundException_whenProductDoesNotExist() {
            // arrange
            every { productRepository.findByIdAndDeletedAtIsNull(999L) } returns null

            // act & assert
            assertThatThrownBy { productService.findById(999L) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)

            verify(exactly = 1) { productRepository.findByIdAndDeletedAtIsNull(999L) }
        }
    }

    @DisplayName("findAll")
    @Nested
    inner class FindAll {
        @DisplayName("상품 목록을 페이징으로 조회한다")
        @Test
        fun returnsProductPage_whenCalled() {
            // arrange
            val products = listOf(
                ProductModel(name = "상품A", price = 10000L, brandId = 1L),
                ProductModel(name = "상품B", price = 20000L, brandId = 2L),
            )
            val pageable = PageRequest.of(0, 10)
            every {
                productRepository.findAllByDeletedAtIsNull(null, pageable)
            } returns PageImpl(products, pageable, 2)

            // act
            val result = productService.findAll(null, pageable)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].name).isEqualTo("상품A")
            assertThat(result.content[1].name).isEqualTo("상품B")
            verify(exactly = 1) { productRepository.findAllByDeletedAtIsNull(null, pageable) }
        }

        @DisplayName("브랜드별 필터링으로 조회한다")
        @Test
        fun returnsFilteredProducts_whenBrandIdProvided() {
            // arrange
            val products = listOf(
                ProductModel(name = "브랜드1 상품A", price = 10000L, brandId = 1L),
                ProductModel(name = "브랜드1 상품B", price = 20000L, brandId = 1L),
            )
            val pageable = PageRequest.of(0, 10)
            every {
                productRepository.findAllByDeletedAtIsNull(1L, pageable)
            } returns PageImpl(products, pageable, 2)

            // act
            val result = productService.findAll(1L, pageable)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.content).allSatisfy { assertThat(it.brandId).isEqualTo(1L) }
            verify(exactly = 1) { productRepository.findAllByDeletedAtIsNull(1L, pageable) }
        }
    }

    @DisplayName("findAllByIds")
    @Nested
    inner class FindAllByIds {
        @DisplayName("여러 상품 ID로 일괄 조회한다")
        @Test
        fun returnsProducts_whenIdsProvided() {
            // arrange
            val products = listOf(
                ProductModel(name = "상품A", price = 10000L, brandId = 1L),
                ProductModel(name = "상품B", price = 20000L, brandId = 2L),
            )
            every {
                productRepository.findAllByIdInAndDeletedAtIsNull(listOf(1L, 2L))
            } returns products

            // act
            val result = productService.findAllByIds(listOf(1L, 2L))

            // assert
            assertThat(result).hasSize(2)
            verify(exactly = 1) { productRepository.findAllByIdInAndDeletedAtIsNull(listOf(1L, 2L)) }
        }
    }

    @DisplayName("incrementLikesCount")
    @Nested
    inner class IncrementLikesCount {
        @DisplayName("상품의 좋아요 수를 증가시킨다")
        @Test
        fun incrementsLikesCount_whenProductExists() {
            // arrange
            val product = ProductModel(
                name = VALID_NAME,
                price = VALID_PRICE,
                brandId = VALID_BRAND_ID,
            )
            every { productRepository.findByIdAndDeletedAtIsNull(1L) } returns product
            every { productRepository.save(any()) } answers { firstArg() }

            // act
            productService.incrementLikesCount(1L)

            // assert
            assertThat(product.likesCount).isEqualTo(1L)
            verify(exactly = 1) { productRepository.save(any()) }
        }
    }

    @DisplayName("decrementLikesCount")
    @Nested
    inner class DecrementLikesCount {
        @DisplayName("상품의 좋아요 수를 감소시킨다")
        @Test
        fun decrementsLikesCount_whenProductExists() {
            // arrange
            val product = ProductModel(
                name = VALID_NAME,
                price = VALID_PRICE,
                brandId = VALID_BRAND_ID,
                likesCount = 5L,
            )
            every { productRepository.findByIdAndDeletedAtIsNull(1L) } returns product
            every { productRepository.save(any()) } answers { firstArg() }

            // act
            productService.decrementLikesCount(1L)

            // assert
            assertThat(product.likesCount).isEqualTo(4L)
            verify(exactly = 1) { productRepository.save(any()) }
        }
    }
}
