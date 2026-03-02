package com.loopers.application.brand

import com.loopers.support.error.BrandErrorCode
import com.loopers.support.error.CoreException
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
class UpdateBrandUseCaseTest @Autowired constructor(
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val updateBrandUseCase: UpdateBrandUseCase,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("브랜드 수정")
    @Nested
    inner class Execute {

        @DisplayName("새 이름으로 수정하면 성공한다")
        @Test
        fun success() {
            val brand = registerBrandUseCase.execute(BrandCommand.Register(name = "나이키"))

            val updated = updateBrandUseCase.execute(BrandCommand.Update(brandId = brand.id, name = "뉴나이키"))

            assertThat(updated.name).isEqualTo("뉴나이키")
        }

        @DisplayName("같은 이름으로 수정하면 중복 검증 없이 성공한다")
        @Test
        fun successWhenSameName() {
            val brand = registerBrandUseCase.execute(BrandCommand.Register(name = "나이키"))

            val updated = updateBrandUseCase.execute(BrandCommand.Update(brandId = brand.id, name = "나이키"))

            assertThat(updated.name).isEqualTo("나이키")
        }

        @DisplayName("다른 활성 브랜드와 같은 이름으로 수정하면 DUPLICATE_BRAND_NAME 예외가 발생한다")
        @Test
        fun failWhenNameAlreadyExists() {
            registerBrandUseCase.execute(BrandCommand.Register(name = "나이키"))
            val adidas = registerBrandUseCase.execute(BrandCommand.Register(name = "아디다스"))

            val exception = assertThrows<CoreException> {
                updateBrandUseCase.execute(BrandCommand.Update(brandId = adidas.id, name = "나이키"))
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.DUPLICATE_BRAND_NAME)
        }
    }
}
