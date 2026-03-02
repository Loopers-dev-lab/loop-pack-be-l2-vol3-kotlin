package com.loopers.application.catalog.product

import com.loopers.domain.catalog.brand.FakeBrandRepository
import com.loopers.domain.catalog.brand.model.Brand
import com.loopers.domain.catalog.brand.vo.BrandName
import com.loopers.domain.catalog.product.FakeProductRepository
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.vo.Stock
import com.loopers.domain.common.vo.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class GetProductUseCaseTest {

    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var productRepository: FakeProductRepository
    private lateinit var useCase: GetProductUseCase

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        productRepository = FakeProductRepository()
        useCase = GetProductUseCase(productRepository, brandRepository)
    }

    @Nested
    @DisplayName("상품 상세 조회 시")
    inner class Execute {

        @Test
        @DisplayName("CatalogInfo(Product + Brand)를 반환한다")
        fun getProduct_returnsProductWithBrand() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))
            val product = productRepository.save(
                Product(refBrandId = brand.id, name = "에어맥스 90", price = Money(BigDecimal("129000")), stock = Stock(100)),
            )

            // act
            val result = useCase.execute(product.id.value)

            // assert
            assertThat(result.product.name).isEqualTo("에어맥스 90")
            assertThat(result.brandName).isEqualTo("나이키")
        }

        @Test
        @DisplayName("삭제된 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        fun getProduct_deletedProduct_throwsNotFound() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))
            val product = productRepository.save(
                Product(refBrandId = brand.id, name = "에어맥스 90", price = Money(BigDecimal("129000")), stock = Stock(100)),
            )
            product.delete()
            productRepository.save(product)

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(product.id.value)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("HIDDEN 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        fun getProduct_hiddenProduct_throwsNotFound() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))
            val product = productRepository.save(
                Product(refBrandId = brand.id, name = "에어맥스 90", price = Money(BigDecimal("129000")), stock = Stock(100)),
            )
            product.update(null, null, null, Product.ProductStatus.HIDDEN)
            productRepository.save(product)

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(product.id.value)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("존재하지 않는 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        fun getProduct_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("삭제된 브랜드의 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        fun getProduct_deletedBrand_throwsNotFound() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))
            val product = productRepository.save(
                Product(refBrandId = brand.id, name = "에어맥스 90", price = Money(BigDecimal("129000")), stock = Stock(100)),
            )
            brand.delete()
            brandRepository.save(brand)

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(product.id.value)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
