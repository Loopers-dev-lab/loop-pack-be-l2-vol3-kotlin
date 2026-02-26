package com.loopers.application.product

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.support.error.CoreException
import com.loopers.support.error.ProductErrorCode
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class GetProductUseCaseTest @Autowired constructor(
    private val registerProductUseCase: RegisterProductUseCase,
    private val getProductUseCase: GetProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerBrand(name: String = "나이키"): Long {
        return registerBrandUseCase.execute(BrandCommand.Register(name = name)).id
    }

    private fun registerProduct(brandId: Long, name: String = "테스트 상품"): ProductInfo {
        return registerProductUseCase.execute(
            ProductCommand.Register(
                brandId = brandId,
                name = name,
                description = "상품 설명",
                price = 10000,
                stock = 100,
                imageUrl = "https://example.com/image.jpg",
            ),
        )
    }

    @DisplayName("상품 단건 조회")
    @Nested
    inner class Execute {

        @DisplayName("존재하는 활성 상품을 조회하면 성공한다")
        @Test
        fun success() {
            val brandId = registerBrand()
            val product = registerProduct(brandId)

            val result = getProductUseCase.execute(product.id)

            assertAll(
                { assertThat(result.name).isEqualTo("테스트 상품") },
                { assertThat(result.brandName).isEqualTo("나이키") },
            )
        }

        @DisplayName("존재하지 않는 상품을 조회하면 PRODUCT_NOT_FOUND 예외가 발생한다")
        @Test
        fun failWhenNotFound() {
            val exception = assertThrows<CoreException> {
                getProductUseCase.execute(999L)
            }

            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND)
        }

        @DisplayName("삭제된 상품을 조회하면 PRODUCT_NOT_FOUND 예외가 발생한다")
        @Test
        fun failWhenDeleted() {
            val brandId = registerBrand()
            val product = registerProduct(brandId)
            deleteProductUseCase.execute(product.id)

            val exception = assertThrows<CoreException> {
                getProductUseCase.execute(product.id)
            }

            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND)
        }
    }
}
