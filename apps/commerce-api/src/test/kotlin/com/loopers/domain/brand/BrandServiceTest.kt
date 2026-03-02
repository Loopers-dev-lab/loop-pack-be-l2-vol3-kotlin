package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageRequest

class BrandServiceTest {

    private lateinit var brandService: BrandService
    private lateinit var brandRepository: FakeBrandRepository

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        brandService = BrandService(brandRepository)
    }

    @Nested
    inner class CreateBrand {

        @Test
        @DisplayName("올바른 정보로 브랜드를 생성하면 성공한다")
        fun success() {
            // arrange
            val command = CreateBrandCommand(
                name = "나이키",
                description = "스포츠 브랜드",
                imageUrl = "https://example.com/nike.png",
            )

            // act
            val brand = brandService.createBrand(command)

            // assert
            assertThat(brand.name).isEqualTo("나이키")
            assertThat(brand.description).isEqualTo("스포츠 브랜드")
            assertThat(brand.id).isGreaterThan(0)
        }

        @Test
        @DisplayName("브랜드명이 빈칸이면 BAD_REQUEST 예외가 발생한다")
        fun nameBlankThrowsBadRequest() {
            // arrange
            val command = CreateBrandCommand(
                name = "   ",
                description = null,
                imageUrl = null,
            )

            // act
            val result = assertThrows<CoreException> {
                brandService.createBrand(command)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class FindById {

        @Test
        @DisplayName("존재하는 브랜드를 조회하면 성공한다")
        fun success() {
            // arrange
            val command = CreateBrandCommand(
                name = "나이키",
                description = "스포츠 브랜드",
                imageUrl = null,
            )
            val saved = brandService.createBrand(command)

            // act
            val found = brandService.findById(saved.id)

            // assert
            assertThat(found.name).isEqualTo("나이키")
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 조회하면 NOT_FOUND 예외가 발생한다")
        fun notFoundThrowsNotFound() {
            // act
            val result = assertThrows<CoreException> {
                brandService.findById(999L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class UpdateBrand {

        @Test
        @DisplayName("존재하는 브랜드를 수정하면 성공한다")
        fun success() {
            // arrange
            val created = brandService.createBrand(
                CreateBrandCommand("나이키", "스포츠", null),
            )
            val command = UpdateBrandCommand(
                name = "아디다스",
                description = "독일 브랜드",
                imageUrl = "https://example.com/adidas.png",
            )

            // act
            val updated = brandService.updateBrand(created.id, command)

            // assert
            assertThat(updated.name).isEqualTo("아디다스")
            assertThat(updated.description).isEqualTo("독일 브랜드")
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 수정하면 NOT_FOUND 예외가 발생한다")
        fun notFoundThrowsNotFound() {
            // arrange
            val command = UpdateBrandCommand(
                name = "아디다스",
                description = null,
                imageUrl = null,
            )

            // act
            val result = assertThrows<CoreException> {
                brandService.updateBrand(999L, command)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class DeleteBrand {

        @Test
        @DisplayName("존재하는 브랜드를 삭제하면 성공한다")
        fun success() {
            // arrange
            val created = brandService.createBrand(
                CreateBrandCommand("나이키", null, null),
            )

            // act
            brandService.deleteBrand(created.id)

            // assert
            val result = assertThrows<CoreException> {
                brandService.findById(created.id)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 삭제하면 NOT_FOUND 예외가 발생한다")
        fun notFoundThrowsNotFound() {
            // act
            val result = assertThrows<CoreException> {
                brandService.deleteBrand(999L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class FindAll {

        @Test
        @DisplayName("등록된 브랜드 목록을 조회하면 성공한다")
        fun success() {
            // arrange
            brandService.createBrand(CreateBrandCommand("나이키", null, null))
            brandService.createBrand(CreateBrandCommand("아디다스", null, null))

            // act
            val brands = brandService.findAll()

            // assert
            assertThat(brands).hasSize(2)
        }

        @Test
        @DisplayName("삭제된 브랜드는 목록에서 제외된다")
        fun excludeDeleted() {
            // arrange
            val created = brandService.createBrand(CreateBrandCommand("나이키", null, null))
            brandService.createBrand(CreateBrandCommand("아디다스", null, null))
            brandService.deleteBrand(created.id)

            // act
            val brands = brandService.findAll()

            // assert
            assertThat(brands).hasSize(1)
            assertThat(brands[0].name).isEqualTo("아디다스")
        }
    }

    @Nested
    inner class FindAllPaged {

        @Test
        @DisplayName("페이징하여 브랜드 목록을 조회하면 성공한다")
        fun success() {
            // arrange
            brandService.createBrand(CreateBrandCommand("나이키", null, null))
            brandService.createBrand(CreateBrandCommand("아디다스", null, null))
            brandService.createBrand(CreateBrandCommand("푸마", null, null))

            // act
            val page = brandService.findAll(PageRequest.of(0, 2))

            // assert
            assertAll(
                { assertThat(page.content).hasSize(2) },
                { assertThat(page.totalElements).isEqualTo(3) },
                { assertThat(page.totalPages).isEqualTo(2) },
                { assertThat(page.number).isEqualTo(0) },
                { assertThat(page.size).isEqualTo(2) },
            )
        }

        @Test
        @DisplayName("두 번째 페이지를 조회하면 나머지 항목이 반환된다")
        fun secondPage() {
            // arrange
            brandService.createBrand(CreateBrandCommand("나이키", null, null))
            brandService.createBrand(CreateBrandCommand("아디다스", null, null))
            brandService.createBrand(CreateBrandCommand("푸마", null, null))

            // act
            val page = brandService.findAll(PageRequest.of(1, 2))

            // assert
            assertAll(
                { assertThat(page.content).hasSize(1) },
                { assertThat(page.content[0].name).isEqualTo("푸마") },
                { assertThat(page.totalElements).isEqualTo(3) },
                { assertThat(page.number).isEqualTo(1) },
            )
        }

        @Test
        @DisplayName("삭제된 브랜드는 페이징 결과에서 제외된다")
        fun excludeDeleted() {
            // arrange
            val created = brandService.createBrand(CreateBrandCommand("나이키", null, null))
            brandService.createBrand(CreateBrandCommand("아디다스", null, null))
            brandService.deleteBrand(created.id)

            // act
            val page = brandService.findAll(PageRequest.of(0, 20))

            // assert
            assertAll(
                { assertThat(page.content).hasSize(1) },
                { assertThat(page.totalElements).isEqualTo(1) },
                { assertThat(page.content[0].name).isEqualTo("아디다스") },
            )
        }
    }
}
