package com.loopers.domain.brand

import com.loopers.support.error.BrandErrorCode
import com.loopers.support.error.BrandException
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
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

    @DisplayName("브랜드명 중복 검증")
    @Nested
    inner class ValidateUniqueName {

        @DisplayName("활성 브랜드에 동일한 이름이 없으면 예외가 발생하지 않는다")
        @Test
        fun successWhenNameNotExists() {
            assertDoesNotThrow {
                brandService.validateUniqueName("나이키")
            }
        }

        @DisplayName("활성 브랜드에 동일한 이름이 있으면 DUPLICATE_BRAND_NAME 예외가 발생한다")
        @Test
        fun failWhenNameAlreadyExists() {
            // arrange
            val brand = Brand.create(name = "나이키")
            brandRepository.save(brand)

            // act & assert
            val exception = assertThrows<BrandException> {
                brandService.validateUniqueName("나이키")
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.DUPLICATE_BRAND_NAME)
        }

        @DisplayName("삭제된 브랜드의 이름은 중복으로 취급하지 않는다")
        @Test
        fun successWhenDeletedBrandHasSameName() {
            // arrange
            val brand = Brand.create(name = "나이키")
            val saved = brandRepository.save(brand)
            saved.delete()
            brandRepository.save(saved)

            // assert
            assertDoesNotThrow {
                brandService.validateUniqueName("나이키")
            }
        }
    }
}
