package com.loopers.application.brand

import com.loopers.domain.brand.BrandStatus
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import com.loopers.infrastructure.brand.BrandJpaRepository
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
    private val brandJpaRepository: BrandJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createCommand(
        name: String = "루퍼스",
        description: String = "루퍼스 브랜드 설명",
        imageUrl: String = "https://example.com/brand.jpg",
    ) = BrandCommand.Create(name = name, description = description, imageUrl = imageUrl)

    @DisplayName("브랜드를 등록할 때,")
    @Nested
    inner class CreateBrand {
        @DisplayName("유효한 정보가 주어지면, 정상적으로 등록된다.")
        @Test
        fun createsBrand_whenValidInfoIsProvided() {
            // act
            val brand = brandService.createBrand(createCommand())

            // assert
            val saved = brandJpaRepository.findById(brand.id).orElse(null)
            assertAll(
                { assertThat(saved).isNotNull() },
                { assertThat(saved!!.name).isEqualTo("루퍼스") },
                { assertThat(saved!!.description).isEqualTo("루퍼스 브랜드 설명") },
                { assertThat(saved!!.imageUrl).isEqualTo("https://example.com/brand.jpg") },
                { assertThat(saved!!.status).isEqualTo(BrandStatus.ACTIVE) },
            )
        }
    }

    @DisplayName("어드민 브랜드를 조회할 때,")
    @Nested
    inner class GetBrandForAdmin {
        @DisplayName("존재하는 ID로 조회하면, 브랜드를 반환한다.")
        @Test
        fun returnsBrand_whenIdExists() {
            // arrange
            val created = brandService.createBrand(createCommand())

            // act
            val result = brandService.getBrandForAdmin(created.id)

            // assert
            assertThat(result.name).isEqualTo("루퍼스")
        }

        @DisplayName("삭제된 브랜드도 조회된다.")
        @Test
        fun returnsDeletedBrand_whenIdExists() {
            // arrange
            val created = brandService.createBrand(createCommand())
            brandService.deleteBrand(created.id)

            // act
            val result = brandService.getBrandForAdmin(created.id)

            // assert
            assertThat(result.status).isEqualTo(BrandStatus.DELETED)
        }

        @DisplayName("존재하지 않는 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenIdDoesNotExist() {
            val result = assertThrows<CoreException> {
                brandService.getBrandForAdmin(999L)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("고객 브랜드를 조회할 때,")
    @Nested
    inner class GetBrand {
        @DisplayName("ACTIVE 브랜드를 조회하면, 브랜드를 반환한다.")
        @Test
        fun returnsBrand_whenBrandIsActive() {
            // arrange
            val created = brandService.createBrand(createCommand())

            // act
            val result = brandService.getBrand(created.id)

            // assert
            assertThat(result.name).isEqualTo("루퍼스")
        }

        @DisplayName("삭제된 브랜드를 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandIsDeleted() {
            // arrange
            val created = brandService.createBrand(createCommand())
            brandService.deleteBrand(created.id)

            // act & assert
            val result = assertThrows<CoreException> {
                brandService.getBrand(created.id)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("브랜드를 수정할 때,")
    @Nested
    inner class UpdateBrand {
        @DisplayName("유효한 정보가 주어지면, 정상적으로 수정된다.")
        @Test
        fun updatesBrand_whenValidInfoIsProvided() {
            // arrange
            val created = brandService.createBrand(createCommand())
            val updateCommand = BrandCommand.Update(
                name = "새 브랜드",
                description = "새 설명",
                imageUrl = "https://example.com/new.jpg",
            )

            // act
            brandService.updateBrand(created.id, updateCommand)

            // assert
            val updated = brandJpaRepository.findById(created.id).orElse(null)
            assertAll(
                { assertThat(updated!!.name).isEqualTo("새 브랜드") },
                { assertThat(updated!!.description).isEqualTo("새 설명") },
                { assertThat(updated!!.imageUrl).isEqualTo("https://example.com/new.jpg") },
            )
        }
    }

    @DisplayName("브랜드를 삭제할 때,")
    @Nested
    inner class DeleteBrand {
        @DisplayName("존재하는 브랜드를 삭제하면, 상태가 DELETED로 변경된다.")
        @Test
        fun deletesBrand_whenBrandExists() {
            // arrange
            val created = brandService.createBrand(createCommand())

            // act
            brandService.deleteBrand(created.id)

            // assert
            val deleted = brandJpaRepository.findById(created.id).orElse(null)
            assertAll(
                { assertThat(deleted!!.status).isEqualTo(BrandStatus.DELETED) },
                { assertThat(deleted!!.deletedAt).isNotNull() },
            )
        }
    }

    @DisplayName("브랜드 목록을 조회할 때,")
    @Nested
    inner class GetBrands {
        @DisplayName("브랜드가 존재하면, 페이징된 결과를 반환한다.")
        @Test
        fun returnsPaginatedBrands_whenBrandsExist() {
            // arrange
            brandService.createBrand(createCommand(name = "브랜드1"))
            brandService.createBrand(createCommand(name = "브랜드2"))
            brandService.createBrand(createCommand(name = "브랜드3"))

            // act
            val result = brandService.getBrands(0, 2)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.totalElements).isEqualTo(3) },
                { assertThat(result.totalPages).isEqualTo(2) },
            )
        }
    }
}
