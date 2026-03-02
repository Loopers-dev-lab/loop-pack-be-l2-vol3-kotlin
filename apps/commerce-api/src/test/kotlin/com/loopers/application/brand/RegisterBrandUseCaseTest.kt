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
class RegisterBrandUseCaseTest @Autowired constructor(
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val deleteBrandUseCase: DeleteBrandUseCase,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("브랜드 등록")
    @Nested
    inner class Execute {

        @DisplayName("이름이 주어지면 등록에 성공한다")
        @Test
        fun success() {
            val brand = registerBrandUseCase.execute(BrandCommand.Register(name = "나이키"))

            assertThat(brand.id).isGreaterThan(0)
        }

        @DisplayName("이미 존재하는 활성 브랜드명으로 등록하면 DUPLICATE_BRAND_NAME 예외가 발생한다")
        @Test
        fun failWhenNameAlreadyExists() {
            registerBrandUseCase.execute(BrandCommand.Register(name = "나이키"))

            val exception = assertThrows<CoreException> {
                registerBrandUseCase.execute(BrandCommand.Register(name = "나이키"))
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.DUPLICATE_BRAND_NAME)
        }

        @DisplayName("삭제된 브랜드의 이름으로 재등록할 수 있다")
        @Test
        fun canReuseDeletedBrandName() {
            val original = registerBrandUseCase.execute(BrandCommand.Register(name = "나이키"))
            deleteBrandUseCase.execute(original.id)

            val reused = registerBrandUseCase.execute(BrandCommand.Register(name = "나이키"))

            assertThat(reused.id).isNotEqualTo(original.id)
        }
    }
}
