package com.loopers.application.brand

import com.loopers.support.error.BrandErrorCode
import com.loopers.support.error.BrandException
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
class GetBrandUseCaseTest @Autowired constructor(
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val deleteBrandUseCase: DeleteBrandUseCase,
    private val getBrandUseCase: GetBrandUseCase,
    private val getAllBrandsUseCase: GetAllBrandsUseCase,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("브랜드 단건 조회")
    @Nested
    inner class GetBrand {

        @DisplayName("존재하는 활성 브랜드를 조회하면 성공한다")
        @Test
        fun success() {
            val brand = registerBrandUseCase.execute(BrandCommand.Register(name = "나이키"))

            val result = getBrandUseCase.execute(brand.id)

            assertThat(result.name).isEqualTo("나이키")
        }

        @DisplayName("존재하지 않는 브랜드를 조회하면 BRAND_NOT_FOUND 예외가 발생한다")
        @Test
        fun failWhenNotFound() {
            val exception = assertThrows<BrandException> {
                getBrandUseCase.execute(999L)
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.BRAND_NOT_FOUND)
        }
    }

    @DisplayName("브랜드 전체 조회")
    @Nested
    inner class GetAllBrands {

        @DisplayName("활성 브랜드만 조회된다")
        @Test
        fun returnsOnlyActiveBrands() {
            registerBrandUseCase.execute(BrandCommand.Register(name = "나이키"))
            val adidas = registerBrandUseCase.execute(BrandCommand.Register(name = "아디다스"))
            deleteBrandUseCase.execute(adidas.id)

            val brands = getAllBrandsUseCase.execute()

            assertThat(brands).hasSize(1)
        }
    }
}
