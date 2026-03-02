package com.loopers.application.catalog.product

import com.loopers.domain.catalog.brand.FakeBrandRepository
import com.loopers.domain.catalog.brand.model.Brand
import com.loopers.domain.catalog.brand.vo.BrandName
import com.loopers.domain.catalog.product.FakeProductRepository
import com.loopers.domain.catalog.product.model.Product
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class CreateProductUseCaseTest {

    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var productRepository: FakeProductRepository
    private lateinit var useCase: CreateProductUseCase

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        productRepository = FakeProductRepository()
        useCase = CreateProductUseCase(productRepository, brandRepository)
    }

    @Nested
    @DisplayName("상품 생성 시")
    inner class Execute {

        @Test
        @DisplayName("유효한 정보로 생성하면 Product가 저장되고 반환된다")
        fun createProduct_withValidData_savesAndReturnsProduct() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))

            // act
            val result = useCase.execute(brand.id.value, "에어맥스 90", BigDecimal("129000"), 100)

            // assert
            assertThat(result.name).isEqualTo("에어맥스 90")
            assertThat(result.price).isEqualByComparingTo(BigDecimal("129000"))
            assertThat(result.stock).isEqualTo(100)
            assertThat(result.status).isEqualTo(Product.ProductStatus.ON_SALE.name)
        }

        @Test
        @DisplayName("존재하지 않는 브랜드로 생성하면 NOT_FOUND 예외가 발생한다")
        fun createProduct_nonExistentBrand_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(999L, "에어맥스 90", BigDecimal("129000"), 100)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("삭제된 브랜드로 생성하면 NOT_FOUND 예외가 발생한다")
        fun createProduct_deletedBrand_throwsNotFound() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))
            brand.delete()
            brandRepository.save(brand)

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(brand.id.value, "에어맥스 90", BigDecimal("129000"), 100)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
