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
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class DeleteProductUseCaseTest @Autowired constructor(
    private val registerProductUseCase: RegisterProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val getProductUseCase: GetProductUseCase,
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

    private fun registerProduct(brandId: Long): ProductInfo {
        return registerProductUseCase.execute(
            ProductCommand.Register(
                brandId = brandId,
                name = "테스트 상품",
                description = "상품 설명",
                price = 10000,
                stock = 100,
                imageUrl = "https://example.com/image.jpg",
            ),
        )
    }

    @DisplayName("상품 삭제")
    @Nested
    inner class Execute {

        @DisplayName("삭제하면 조회되지 않는다")
        @Test
        fun success() {
            val brandId = registerBrand()
            val product = registerProduct(brandId)

            deleteProductUseCase.execute(product.id)

            val exception = assertThrows<CoreException> {
                getProductUseCase.execute(product.id)
            }
            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND)
        }

        @DisplayName("이미 삭제된 상품을 다시 삭제하면 PRODUCT_NOT_FOUND 예외가 발생한다")
        @Test
        fun failWhenAlreadyDeleted() {
            val brandId = registerBrand()
            val product = registerProduct(brandId)
            deleteProductUseCase.execute(product.id)

            val exception = assertThrows<CoreException> {
                deleteProductUseCase.execute(product.id)
            }
            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND)
        }
    }
}
