package com.loopers.application.brand

import com.loopers.application.product.GetProductUseCase
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.support.error.BrandErrorCode
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
class DeleteBrandUseCaseTest @Autowired constructor(
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val deleteBrandUseCase: DeleteBrandUseCase,
    private val getBrandUseCase: GetBrandUseCase,
    private val registerProductUseCase: RegisterProductUseCase,
    private val getProductUseCase: GetProductUseCase,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("브랜드 삭제")
    @Nested
    inner class Execute {

        @DisplayName("삭제하면 조회되지 않는다")
        @Test
        fun success() {
            val brand = registerBrandUseCase.execute(BrandCommand.Register(name = "나이키"))

            deleteBrandUseCase.execute(brand.id)

            val exception = assertThrows<CoreException> {
                getBrandUseCase.execute(brand.id)
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.BRAND_NOT_FOUND)
        }

        @DisplayName("이미 삭제된 브랜드를 다시 삭제하면 BRAND_NOT_FOUND 예외가 발생한다")
        @Test
        fun failWhenAlreadyDeleted() {
            val brand = registerBrandUseCase.execute(BrandCommand.Register(name = "나이키"))
            deleteBrandUseCase.execute(brand.id)

            val exception = assertThrows<CoreException> {
                deleteBrandUseCase.execute(brand.id)
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.BRAND_NOT_FOUND)
        }

        @DisplayName("브랜드 삭제 시 소속 상품도 함께 소프트 삭제된다")
        @Test
        fun cascadeDeleteProducts() {
            val brand = registerBrandUseCase.execute(BrandCommand.Register(name = "나이키"))
            val product = registerProductUseCase.execute(
                ProductCommand.Register(
                    brandId = brand.id,
                    name = "에어맥스",
                    description = "설명",
                    price = 10000,
                    stock = 100,
                    imageUrl = "https://example.com/image.jpg",
                ),
            )

            deleteBrandUseCase.execute(brand.id)

            val exception = assertThrows<CoreException> {
                getProductUseCase.execute(product.id)
            }
            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND)
        }
    }
}
