package com.loopers.application.product

import com.loopers.application.brand.BrandService
import com.loopers.domain.brand.Brand
import com.loopers.domain.product.CreateProductCommand
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
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class ProductFacadeTest {

    @Mock
    private lateinit var productService: ProductService

    @Mock
    private lateinit var brandService: BrandService

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
            val command = CreateProductCommand(
                brandId = brandId,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = 100,
                description = "나이키 에어맥스 90",
                imageUrl = "https://example.com/airmax90.jpg",
            )
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")
            val product = Product(
                brandId = brandId,
                name = command.name,
                price = command.price,
                stock = command.stock,
                description = command.description,
                imageUrl = command.imageUrl,
            )

            whenever(brandService.getBrand(brandId)).thenReturn(brand)
            whenever(productService.createProduct(any())).thenReturn(product)

            // act
            val result = productFacade.createProduct(command)

            // assert
            assertThat(result.name).isEqualTo("에어맥스 90")
        }

        @DisplayName("브랜드가 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsException_whenBrandNotFound() {
            // arrange
            val command = CreateProductCommand(
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
                productFacade.createProduct(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
