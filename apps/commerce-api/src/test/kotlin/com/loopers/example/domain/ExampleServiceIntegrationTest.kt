package com.loopers.example.domain

import com.loopers.example.application.ExampleService
import com.loopers.example.infrastructure.ExampleJpaRepository
import com.loopers.example.infrastructure.ExampleEntity
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
class ExampleServiceIntegrationTest @Autowired constructor(
    private val exampleService: ExampleService,
    private val exampleJpaRepository: ExampleJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("예시를 조회할 때,")
    @Nested
    inner class Get {
        @DisplayName("존재하는 예시 ID를 주면, 해당 예시 정보를 반환한다.")
        @Test
        fun returnsExampleInfo_whenValidIdIsProvided() {
            // arrange
            val exampleEntity = exampleJpaRepository.save(ExampleEntity(id = null, name = "예시 제목", description = "예시 설명"))

            // act
            val result = exampleService.getExample(exampleEntity.id!!)

            // assert
            assertAll(
                { assertThat(result).isNotNull() },
                { assertThat(result.id).isEqualTo(exampleEntity.id) },
                { assertThat(result.name).isEqualTo(exampleEntity.name) },
                { assertThat(result.description).isEqualTo(exampleEntity.description) },
            )
        }

        @DisplayName("존재하지 않는 예시 ID를 주면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsException_whenInvalidIdIsProvided() {
            // arrange
            val invalidId = 999L // Assuming this ID does not exist

            // act
            val exception = assertThrows<CoreException> {
                exampleService.getExample(invalidId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
