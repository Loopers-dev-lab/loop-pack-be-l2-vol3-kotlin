package com.loopers.application.product

import com.loopers.application.brand.BrandService
import com.loopers.application.like.LikeService
import com.loopers.domain.brand.Brand
import com.loopers.domain.like.Like
import com.loopers.domain.product.Product
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class ProductFacadeTest {

    @Mock
    private lateinit var productService: ProductService

    @Mock
    private lateinit var brandService: BrandService

    @Mock
    private lateinit var likeService: LikeService

    @InjectMocks
    private lateinit var productFacade: ProductFacade

    @DisplayName("상품을 등록할 때,")
    @Nested
    inner class CreateProduct {

        @DisplayName("브랜드가 존재하면, 상품이 생성된다.")
        @Test
        fun createsProduct_whenBrandExists() {
            // arrange
            val brandId = 1L
            val criteria = CreateProductCriteria(
                brandId = brandId,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = 100,
                description = "나이키 에어맥스 90",
                imageUrl = "https://example.com/airmax90.jpg",
            )
            val now = ZonedDateTime.now()
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")
            val product = Product(
                brandId = brandId,
                name = criteria.name,
                price = criteria.price,
                stock = criteria.stock,
                description = criteria.description,
                imageUrl = criteria.imageUrl,
            )
            ReflectionTestUtils.setField(product, "createdAt", now)
            ReflectionTestUtils.setField(product, "updatedAt", now)

            whenever(brandService.getBrand(brandId)).thenReturn(brand)
            whenever(productService.createProduct(any())).thenReturn(product)

            // act
            val result = productFacade.createProduct(criteria)

            // assert
            assertThat(result.name).isEqualTo("에어맥스 90")
        }

        @DisplayName("브랜드가 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsException_whenBrandNotFound() {
            // arrange
            val criteria = CreateProductCriteria(
                brandId = 999L,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = 100,
                description = null,
                imageUrl = null,
            )

            whenever(brandService.getBrand(999L)).thenThrow(
                CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."),
            )

            // act
            val exception = assertThrows<CoreException> {
                productFacade.createProduct(criteria)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품에 좋아요를 등록할 때,")
    @Nested
    inner class AddLike {

        @DisplayName("상품이 존재하면, 좋아요가 등록된다.")
        @Test
        fun addsLike_whenProductExists() {
            // arrange
            val userId = 1L
            val productId = 1L
            val now = ZonedDateTime.now()
            val product = Product(
                brandId = 1L,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = 100,
                description = null,
                imageUrl = null,
            )
            val like = Like(userId = userId, productId = productId)
            ReflectionTestUtils.setField(like, "createdAt", now)

            whenever(productService.getProductIncludingDeleted(productId)).thenReturn(product)
            whenever(likeService.addLike(userId, productId)).thenReturn(like)

            // act
            val result = productFacade.addLike(userId, productId)

            // assert
            assertThat(result.productId).isEqualTo(productId)
        }

        @DisplayName("상품이 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsException_whenProductNotFound() {
            // arrange
            val userId = 1L
            val productId = 999L

            whenever(productService.getProductIncludingDeleted(productId)).thenThrow(
                CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."),
            )

            // act
            val exception = assertThrows<CoreException> {
                productFacade.addLike(userId, productId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("삭제된 상품에도, 좋아요가 등록된다.")
        @Test
        fun addsLike_whenProductIsSoftDeleted() {
            // arrange
            val userId = 1L
            val productId = 1L
            val now = ZonedDateTime.now()
            val deletedProduct = Product(
                brandId = 1L,
                name = "삭제된 상품",
                price = BigDecimal("10000"),
                stock = 0,
                description = null,
                imageUrl = null,
            )
            deletedProduct.delete()
            val like = Like(userId = userId, productId = productId)
            ReflectionTestUtils.setField(like, "createdAt", now)

            whenever(productService.getProductIncludingDeleted(productId)).thenReturn(deletedProduct)
            whenever(likeService.addLike(userId, productId)).thenReturn(like)

            // act
            val result = productFacade.addLike(userId, productId)

            // assert
            assertThat(result.productId).isEqualTo(productId)
        }
    }
}
