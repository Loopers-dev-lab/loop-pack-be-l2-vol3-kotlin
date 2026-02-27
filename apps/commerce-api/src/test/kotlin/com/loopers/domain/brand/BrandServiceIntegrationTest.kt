package com.loopers.domain.brand

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

@SpringBootTest
class BrandServiceIntegrationTest @Autowired constructor(
    private val brandService: BrandService,
    private val brandJpaRepository: BrandJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("브랜드를 조회할 때, ")
    @Nested
    inner class GetBrand {
        @DisplayName("존재하는 브랜드 ID를 주면, 브랜드 정보를 반환한다.")
        @Test
        fun returnsBrand_whenBrandExists() {
            // arrange
            val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))

            // act
            val result = brandService.getBrand(brand.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(brand.id) },
                { assertThat(result.name).isEqualTo(brand.name) },
                { assertThat(result.description).isEqualTo(brand.description) },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID를 주면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandNotExists() {
            // arrange
            val invalidId = 999L

            // act
            val exception = assertThrows<CoreException> {
                brandService.getBrand(invalidId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("삭제된 브랜드 ID를 주면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandIsDeleted() {
            // arrange
            val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            brand.delete()
            brandJpaRepository.save(brand)

            // act
            val exception = assertThrows<CoreException> {
                brandService.getBrand(brand.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("브랜드 목록을 조회할 때, ")
    @Nested
    inner class GetBrands {
        @DisplayName("등록된 브랜드가 있으면, 페이지네이션된 목록을 반환한다.")
        @Test
        fun returnsBrandList_whenBrandsExist() {
            // arrange
            brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            brandJpaRepository.save(Brand(name = "아디다스", description = "독일 스포츠 브랜드"))

            // act
            val result = brandService.getBrands(PageRequest.of(0, 20))

            // assert
            assertThat(result.content).hasSize(2)
        }

        @DisplayName("등록된 브랜드가 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyList_whenNoBrandsExist() {
            // act
            val result = brandService.getBrands(PageRequest.of(0, 20))

            // assert
            assertThat(result.content).isEmpty()
        }
    }

    @DisplayName("브랜드를 생성할 때, ")
    @Nested
    inner class CreateBrand {
        @DisplayName("이름과 설명이 주어지면, 브랜드가 생성된다.")
        @Test
        fun createsBrand_whenNameAndDescriptionAreProvided() {
            // arrange
            val name = "나이키"
            val description = "스포츠 브랜드"

            // act
            val result = brandService.createBrand(name, description)

            // assert
            val savedBrand = brandJpaRepository.findById(result.id).get()
            assertAll(
                { assertThat(savedBrand.name).isEqualTo(name) },
                { assertThat(savedBrand.description).isEqualTo(description) },
            )
        }
    }

    @DisplayName("브랜드를 수정할 때, ")
    @Nested
    inner class UpdateBrand {
        @DisplayName("존재하는 브랜드 ID와 새 정보가 주어지면, 수정된다.")
        @Test
        fun updatesBrand_whenBrandExists() {
            // arrange
            val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val newName = "아디다스"
            val newDescription = "독일 스포츠 브랜드"

            // act
            val result = brandService.updateBrand(brand.id, newName, newDescription)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo(newName) },
                { assertThat(result.description).isEqualTo(newDescription) },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID를 주면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandNotExists() {
            // act
            val exception = assertThrows<CoreException> {
                brandService.updateBrand(999L, "아디다스", "독일 스포츠 브랜드")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("브랜드를 삭제할 때, ")
    @Nested
    inner class DeleteBrand {
        @DisplayName("존재하는 브랜드 ID를 주면, soft delete 된다.")
        @Test
        fun softDeletesBrand_whenBrandExists() {
            // arrange
            val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))

            // act
            brandService.deleteBrand(brand.id)

            // assert
            val deletedBrand = brandJpaRepository.findById(brand.id).get()
            assertThat(deletedBrand.deletedAt).isNotNull()
        }

        @DisplayName("존재하지 않는 브랜드 ID를 주면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandNotExists() {
            // act
            val exception = assertThrows<CoreException> {
                brandService.deleteBrand(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
