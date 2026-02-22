package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
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
class BrandServiceIntegrationTest @Autowired constructor(
    private val brandService: BrandService,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val DEFAULT_NAME = "나이키"
        private const val DEFAULT_DESCRIPTION = "스포츠 브랜드"
        private const val DEFAULT_LOGO_URL = "https://example.com/nike-logo.png"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createRegisterCommand(
        name: String = DEFAULT_NAME,
        description: String? = DEFAULT_DESCRIPTION,
        logoUrl: String? = DEFAULT_LOGO_URL,
    ): RegisterCommand = RegisterCommand(
        name = name,
        description = description,
        logoUrl = logoUrl,
    )

    @DisplayName("브랜드 등록")
    @Nested
    inner class Register {

        @DisplayName("유효한 정보가 주어지면, 정상적으로 등록된다.")
        @Test
        fun registersBrandWhenValidInfoIsProvided() {
            // arrange
            val command = createRegisterCommand()

            // act
            val result = brandService.register(command)

            // assert
            assertAll(
                { assertThat(result.id).isNotNull() },
                { assertThat(result.name).isEqualTo(DEFAULT_NAME) },
                { assertThat(result.description).isEqualTo(DEFAULT_DESCRIPTION) },
                { assertThat(result.logoUrl).isEqualTo(DEFAULT_LOGO_URL) },
            )
        }

        @DisplayName("중복된 이름이 주어지면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflictExceptionWhenDuplicateNameIsProvided() {
            // arrange
            brandService.register(createRegisterCommand())
            val duplicateCommand = createRegisterCommand()

            // act
            val result = assertThrows<CoreException> {
                brandService.register(duplicateCommand)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }
}
