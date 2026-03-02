package com.loopers.application.product

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.brand.DeleteBrandUseCase
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
class RegisterProductUseCaseTest @Autowired constructor(
    private val registerProductUseCase: RegisterProductUseCase,
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val deleteBrandUseCase: DeleteBrandUseCase,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerBrand(name: String = "나이키"): Long {
        return registerBrandUseCase.execute(BrandCommand.Register(name = name)).id
    }

    private fun productCommand(brandId: Long): ProductCommand.Register {
        return ProductCommand.Register(
            brandId = brandId,
            name = "테스트 상품",
            description = "상품 설명",
            price = 10000,
            stock = 100,
            imageUrl = "https://example.com/image.jpg",
        )
    }

    @DisplayName("상품 등록")
    @Nested
    inner class Execute {

        @DisplayName("정상 입력이면 등록에 성공한다")
        @Test
        fun success() {
            val brandId = registerBrand()

            val product = registerProductUseCase.execute(productCommand(brandId))

            assertAll(
                { assertThat(product.id).isGreaterThan(0) },
                { assertThat(product.brandName).isEqualTo("나이키") },
                { assertThat(product.name).isEqualTo("테스트 상품") },
            )
        }

        @DisplayName("존재하지 않는 브랜드로 등록하면 INVALID_BRAND 예외가 발생한다")
        @Test
        fun failWhenBrandNotFound() {
            val exception = assertThrows<CoreException> {
                registerProductUseCase.execute(productCommand(999L))
            }

            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.INVALID_BRAND)
        }

        @DisplayName("삭제된 브랜드로 등록하면 INVALID_BRAND 예외가 발생한다")
        @Test
        fun failWhenBrandDeleted() {
            val brandId = registerBrand()
            deleteBrandUseCase.execute(brandId)

            val exception = assertThrows<CoreException> {
                registerProductUseCase.execute(productCommand(brandId))
            }

            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.INVALID_BRAND)
        }
    }
}
