package com.loopers.domain.brand

import com.loopers.support.common.PageQuery
import com.loopers.support.common.SortOrder
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

    @DisplayName("브랜드 생성할 때,")
    @Nested
    inner class CreateBrand {

        @DisplayName("유효한 이름과 설명이 주어지면, DB에 저장되고 조회할 수 있다.")
        @Test
        fun savesBrandToDb_whenValidNameAndDescription() {
            // act
            val result = brandService.createBrand("나이키", "스포츠 브랜드")

            // assert
            val found = brandService.getBrand(result.id)
            assertAll(
                { assertThat(found.name).isEqualTo("나이키") },
                { assertThat(found.description).isEqualTo("스포츠 브랜드") },
            )
        }

        @DisplayName("설명이 null이면, 설명 없이 저장된다.")
        @Test
        fun savesBrandWithNullDescription() {
            // act
            val result = brandService.createBrand("무인양품", null)

            // assert
            val found = brandService.getBrand(result.id)
            assertAll(
                { assertThat(found.name).isEqualTo("무인양품") },
                { assertThat(found.description).isNull() },
            )
        }
    }

    @DisplayName("브랜드 목록 조회할 때,")
    @Nested
    inner class GetBrands {

        @DisplayName("DB에 저장된 브랜드를 페이징으로 조회하면, 해당 페이지의 브랜드를 반환한다.")
        @Test
        fun returnsPagedBrands_whenBrandsExistInDb() {
            // arrange
            brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            brandRepository.save(Brand(name = "아디다스", description = "스포츠 브랜드"))
            brandRepository.save(Brand(name = "무인양품", description = null))
            val pageQuery = PageQuery(0, 2, SortOrder.UNSORTED)

            // act
            val result = brandService.getBrands(pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.totalElements).isEqualTo(3) },
                { assertThat(result.totalPages).isEqualTo(2) },
            )
        }

        @DisplayName("삭제된 브랜드는 목록에 포함되지 않는다.")
        @Test
        fun excludesDeletedBrands() {
            // arrange
            brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val deletedBrand = brandRepository.save(Brand(name = "삭제될 브랜드", description = "설명"))
            deletedBrand.delete()
            brandRepository.save(deletedBrand)
            val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)

            // act
            val result = brandService.getBrands(pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.totalElements).isEqualTo(1) },
                { assertThat(result.content[0].name).isEqualTo("나이키") },
            )
        }

        @DisplayName("브랜드가 없으면, 빈 페이지를 반환한다.")
        @Test
        fun returnsEmptyPage_whenNoBrandsExistInDb() {
            // arrange
            val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)

            // act
            val result = brandService.getBrands(pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).isEmpty() },
                { assertThat(result.totalElements).isEqualTo(0) },
            )
        }
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

    @DisplayName("브랜드 수정할 때,")
    @Nested
    inner class UpdateBrand {

        @DisplayName("유효한 수정 정보가 주어지면, DB에 반영되고 조회할 수 있다.")
        @Test
        fun updatesBrandInDb_whenValidRequest() {
            // arrange
            val saved = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))

            // act
            brandService.updateBrand(saved.id, "아디다스", "독일 스포츠 브랜드")

            // assert
            val found = brandService.getBrand(saved.id)
            assertAll(
                { assertThat(found.name).isEqualTo("아디다스") },
                { assertThat(found.description).isEqualTo("독일 스포츠 브랜드") },
            )
        }

        @DisplayName("설명을 null로 변경하면, DB에 반영된다.")
        @Test
        fun updatesBrandDescriptionToNull() {
            // arrange
            val saved = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))

            // act
            brandService.updateBrand(saved.id, "나이키", null)

            // assert
            val found = brandService.getBrand(saved.id)
            assertAll(
                { assertThat(found.name).isEqualTo("나이키") },
                { assertThat(found.description).isNull() },
            )
        }

        @DisplayName("삭제된 브랜드를 수정하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandIsDeleted() {
            // arrange
            val saved = brandRepository.save(Brand(name = "삭제될 브랜드", description = "설명"))
            saved.delete()
            brandRepository.save(saved)

            // act
            val exception = assertThrows<CoreException> {
                brandService.updateBrand(saved.id, "새 이름", "새 설명")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("존재하지 않는 brandId로 수정하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandNotExistsInDb() {
            // act
            val exception = assertThrows<CoreException> {
                brandService.updateBrand(9999L, "새 이름", "새 설명")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
