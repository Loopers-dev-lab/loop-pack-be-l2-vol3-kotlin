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
class BrandReaderIntegrationTest @Autowired constructor(
    private val brandReader: BrandReader,
    private val brandJpaRepository: BrandJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    inner class GetById {
        @Test
        fun `ID로_브랜드를_조회할_수_있다`() {
            // arrange
            val entity = createAndSaveBrandEntity("나이키")

            // act
            val brand = brandReader.getById(entity.id!!)

            // assert
            assertAll(
                { assertThat(brand.id).isEqualTo(entity.id) },
                { assertThat(brand.name.value).isEqualTo("나이키") },
            )
        }

        @Test
        fun `존재하지_않는_ID면_예외가_발생한다`() {
            // act
            val result = assertThrows<CoreException> {
                brandReader.getById(999L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BRAND_NOT_FOUND)
        }
    }

    @Nested
    inner class GetAllActive {
        @Test
        fun `활성_브랜드만_조회할_수_있다`() {
            // arrange
            createAndSaveBrandEntity("나이키", BrandStatus.ACTIVE.name)
            createAndSaveBrandEntity("아디다스", BrandStatus.ACTIVE.name)
            createAndSaveBrandEntity("비활성브랜드", BrandStatus.INACTIVE.name)

            // act
            val brands = brandReader.getAllActive()

            // assert
            assertThat(brands).hasSize(2)
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
