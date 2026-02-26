package com.loopers.application.catalog.brand

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

class DeleteBrandUseCaseTest {

    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var productRepository: FakeProductRepository
    private lateinit var useCase: DeleteBrandUseCase

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        productRepository = FakeProductRepository()
        useCase = DeleteBrandUseCase(brandRepository, productRepository)
    }

    @Nested
    @DisplayName("브랜드 삭제 시")
    inner class Execute {

        @Test
        @DisplayName("브랜드를 삭제하면 soft delete되고 소속 상품도 cascade 삭제된다")
        fun deleteBrand_softDeletesWithCascade() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))
            val product = productRepository.save(
                Product(refBrandId = brand.id, name = "에어맥스", price = Money(BigDecimal("129000")), stock = Stock(10)),
            )

            // act
            useCase.execute(brand.id.value)

            // assert
            val deletedBrand = brandRepository.findById(brand.id)
            assertThat(deletedBrand?.deletedAt).isNotNull()
            val deletedProduct = productRepository.findById(product.id)
            assertThat(deletedProduct?.deletedAt).isNotNull()
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 삭제하면 NOT_FOUND 예외가 발생한다")
        fun deleteBrand_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("이미 삭제된 브랜드를 삭제하면 NOT_FOUND 예외가 발생한다")
        fun deleteBrand_alreadyDeleted_throwsNotFound() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))
            brand.delete()
            brandRepository.save(brand)

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(brand.id.value)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
