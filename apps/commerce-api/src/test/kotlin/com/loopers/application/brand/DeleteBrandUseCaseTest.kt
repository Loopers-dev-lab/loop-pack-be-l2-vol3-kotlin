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
class DeleteBrandUseCaseTest @Autowired constructor(
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val deleteBrandUseCase: DeleteBrandUseCase,
    private val getBrandUseCase: GetBrandUseCase,
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

            val exception = assertThrows<BrandException> {
                getBrandUseCase.execute(brand.id)
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.BRAND_NOT_FOUND)
        }

        @DisplayName("이미 삭제된 브랜드를 다시 삭제하면 BRAND_NOT_FOUND 예외가 발생한다")
        @Test
        fun failWhenAlreadyDeleted() {
            val brand = registerBrandUseCase.execute(BrandCommand.Register(name = "나이키"))
            deleteBrandUseCase.execute(brand.id)

            val exception = assertThrows<BrandException> {
                deleteBrandUseCase.execute(brand.id)
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.BRAND_NOT_FOUND)
        }
    }
}
