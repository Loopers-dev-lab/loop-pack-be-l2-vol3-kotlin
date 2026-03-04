package com.loopers.domain.brand

import com.loopers.infrastructure.brand.BrandEntity
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BrandRegisterIntegrationTest @Autowired constructor(
    private val brandRegister: BrandRegister,
    private val brandJpaRepository: BrandJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    inner class Register {
        @Test
        fun `신규_브랜드를_등록할_수_있다`() {
            // arrange & act
            val brand = brandRegister.register("나이키")

            // assert
            assertAll(
                { assertThat(brand.id).isNotNull() },
                { assertThat(brand.id).isGreaterThan(0) },
                { assertThat(brand.name.value).isEqualTo("나이키") },
                { assertThat(brand.status).isEqualTo(BrandStatus.ACTIVE) },
            )
        }

        @Test
        fun `이미_존재하는_브랜드명이면_예외가_발생한다`() {
            // arrange
            createAndSaveBrandEntity("나이키")

            // act
            val result = assertThrows<CoreException> {
                brandRegister.register("나이키")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.DUPLICATE_BRAND_NAME)
        }
    }

    private fun createAndSaveBrandEntity(
        name: String = "나이키",
        status: String = BrandStatus.ACTIVE.name,
    ): BrandEntity {
        return brandJpaRepository.save(
            BrandEntity(name = name, status = status),
        )
    }
}
