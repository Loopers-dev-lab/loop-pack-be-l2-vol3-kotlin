package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.CreateBrandCommand
import com.loopers.domain.brand.UpdateBrandCommand
import com.loopers.infrastructure.brand.BrandJpaRepository
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
import org.springframework.data.domain.PageRequest

/**
 * BrandService 통합 테스트
 * - 실제 DB(TestContainers)와 연동하여 Service → Repository 레이어 통합 테스트
 * - @Transactional 경계가 Service에 있으므로 Service를 통해 테스트
 */
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

    @DisplayName("브랜드를 조회할 때,")
    @Nested
    inner class GetBrand {

        @DisplayName("존재하는 브랜드 ID로 조회하면, 브랜드 정보가 반환된다.")
        @Test
        fun returnsBrand_whenBrandExists() {
            // arrange
            val saved = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))

            // act
            val result = brandService.getBrand(saved.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(saved.id) },
                { assertThat(result.name).isEqualTo("나이키") },
                { assertThat(result.description).isEqualTo("스포츠 브랜드") },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandNotExists() {
            // act & assert
            val exception = assertThrows<CoreException> {
                brandService.getBrand(999L)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("soft delete된 브랜드는 조회되지 않는다.")
        @Test
        fun throwsNotFound_whenBrandIsSoftDeleted() {
            // arrange
            val saved = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            saved.delete()
            brandJpaRepository.save(saved)

            // act & assert
            val exception = assertThrows<CoreException> {
                brandService.getBrand(saved.id)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("브랜드 목록을 조회할 때,")
    @Nested
    inner class GetAllBrands {

        @DisplayName("브랜드가 존재하면, 페이징된 목록이 반환된다.")
        @Test
        fun returnsBrandList_whenBrandsExist() {
            // arrange
            brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            brandJpaRepository.save(Brand(name = "아디다스", description = "독일 스포츠 브랜드"))

            // act
            val result = brandService.getAllBrands(PageRequest.of(0, 20))

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.totalElements).isEqualTo(2L) },
            )
        }

        @DisplayName("soft delete된 브랜드는 목록에 포함되지 않는다.")
        @Test
        fun excludesSoftDeletedBrands() {
            // arrange
            brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val deleted = brandJpaRepository.save(Brand(name = "아디다스", description = "독일 스포츠 브랜드"))
            deleted.delete()
            brandJpaRepository.save(deleted)

            // act
            val result = brandService.getAllBrands(PageRequest.of(0, 20))

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content[0].name).isEqualTo("나이키") },
            )
        }
    }

    @DisplayName("브랜드를 등록할 때,")
    @Nested
    inner class CreateBrand {

        @DisplayName("정상적인 정보가 주어지면, 브랜드가 DB에 저장된다.")
        @Test
        fun savesBrandToDatabase_whenValidInfoProvided() {
            // arrange
            val command = CreateBrandCommand(name = "나이키", description = "스포츠 브랜드")

            // act
            val result = brandService.createBrand(command)

            // assert
            val saved = brandJpaRepository.findByIdAndDeletedAtIsNull(result.id)!!
            assertAll(
                { assertThat(saved.name).isEqualTo("나이키") },
                { assertThat(saved.description).isEqualTo("스포츠 브랜드") },
            )
        }

        @DisplayName("이미 존재하는 브랜드명으로 등록하면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenDuplicateName() {
            // arrange
            brandJpaRepository.save(Brand(name = "나이키", description = "기존 브랜드"))
            val command = CreateBrandCommand(name = "나이키", description = "새 브랜드")

            // act & assert
            val exception = assertThrows<CoreException> {
                brandService.createBrand(command)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }

    @DisplayName("브랜드를 수정할 때,")
    @Nested
    inner class UpdateBrand {

        @DisplayName("정상적인 정보가 주어지면, 브랜드가 DB에서 수정된다.")
        @Test
        fun updatesBrandInDatabase_whenValidInfoProvided() {
            // arrange
            val saved = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val command = UpdateBrandCommand(name = "아디다스", description = "독일 스포츠 브랜드")

            // act
            brandService.updateBrand(saved.id, command)

            // assert
            val updated = brandJpaRepository.findByIdAndDeletedAtIsNull(saved.id)!!
            assertAll(
                { assertThat(updated.name).isEqualTo("아디다스") },
                { assertThat(updated.description).isEqualTo("독일 스포츠 브랜드") },
            )
        }

        @DisplayName("다른 브랜드와 같은 이름으로 수정하면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenNameAlreadyExists() {
            // arrange
            brandJpaRepository.save(Brand(name = "아디다스", description = "독일 스포츠 브랜드"))
            val target = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val command = UpdateBrandCommand(name = "아디다스", description = "설명 변경")

            // act & assert
            val exception = assertThrows<CoreException> {
                brandService.updateBrand(target.id, command)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("존재하지 않는 브랜드를 수정하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandNotExists() {
            // arrange
            val command = UpdateBrandCommand(name = "아디다스", description = "설명")

            // act & assert
            val exception = assertThrows<CoreException> {
                brandService.updateBrand(999L, command)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("브랜드를 삭제할 때,")
    @Nested
    inner class DeleteBrand {

        @DisplayName("존재하는 브랜드를 삭제하면, soft delete 되어 조회되지 않는다.")
        @Test
        fun softDeletesBrand_whenBrandExists() {
            // arrange
            val saved = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))

            // act
            brandService.deleteBrand(saved.id)

            // assert
            val deleted = brandJpaRepository.findById(saved.id).get()
            assertAll(
                { assertThat(deleted.isDeleted()).isTrue() },
                { assertThat(brandJpaRepository.findByIdAndDeletedAtIsNull(saved.id)).isNull() },
            )
        }

        @DisplayName("존재하지 않는 브랜드를 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandNotExists() {
            // act & assert
            val exception = assertThrows<CoreException> {
                brandService.deleteBrand(999L)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
