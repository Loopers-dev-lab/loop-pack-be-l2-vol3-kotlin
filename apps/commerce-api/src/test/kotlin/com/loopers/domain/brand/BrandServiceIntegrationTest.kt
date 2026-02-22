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
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("브랜드 조회할 때,")
    @Nested
    inner class GetBrand {

        @DisplayName("DB에 저장된 브랜드를 조회하면, 브랜드 정보를 반환한다.")
        @Test
        fun returnsBrand_whenBrandExistsInDb() {
            // arrange
            val saved = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))

            // act
            val result = brandService.getBrand(saved.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(saved.id) },
                { assertThat(result.name).isEqualTo("나이키") },
                { assertThat(result.description).isEqualTo("스포츠 브랜드") },
            )
        }

        @DisplayName("존재하지 않는 brandId로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandNotExistsInDb() {
            // arrange
            val nonExistentId = 9999L

            // act
            val exception = assertThrows<CoreException> {
                brandService.getBrand(nonExistentId)
            }

            // assert
            assertAll(
                { assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND) },
                { assertThat(exception.message).contains("브랜드를 찾을 수 없습니다") },
            )
        }

        @DisplayName("삭제된 브랜드를 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandIsDeleted() {
            // arrange
            val saved = brandRepository.save(Brand(name = "삭제될 브랜드", description = "설명"))
            saved.delete()
            brandRepository.save(saved)

            // act
            val exception = assertThrows<CoreException> {
                brandService.getBrand(saved.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
