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
class UpdateProductUseCaseTest @Autowired constructor(
    private val registerProductUseCase: RegisterProductUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
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

    @DisplayName("상품 수정")
    @Nested
    inner class Execute {

        @DisplayName("정상 입력이면 수정에 성공한다")
        @Test
        fun success() {
            val brandId = registerBrand()
            val product = registerProduct(brandId)

            val updated = updateProductUseCase.execute(
                ProductCommand.Update(
                    productId = product.id,
                    name = "수정된 상품",
                    description = "수정된 설명",
                    price = 20000,
                    stock = 50,
                    imageUrl = "https://example.com/new.jpg",
                ),
            )

            assertAll(
                { assertThat(updated.name).isEqualTo("수정된 상품") },
                { assertThat(updated.price).isEqualTo(20000) },
                { assertThat(updated.stock).isEqualTo(50) },
            )
        }

        @DisplayName("삭제된 상품을 수정하면 PRODUCT_NOT_FOUND 예외가 발생한다")
        @Test
        fun failWhenDeleted() {
            val brandId = registerBrand()
            val product = registerProduct(brandId)
            deleteProductUseCase.execute(product.id)

            val exception = assertThrows<CoreException> {
                updateProductUseCase.execute(
                    ProductCommand.Update(
                        productId = product.id,
                        name = "수정된 상품",
                        description = "수정된 설명",
                        price = 20000,
                        stock = 50,
                        imageUrl = "https://example.com/new.jpg",
                    ),
                )
            }

            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND)
        }
    }
}
