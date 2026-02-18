package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class BrandTest {

    @DisplayName("브랜드")
    @Nested
    inner class BrandEntity {
        @DisplayName("브랜드의 기본 정보를 올바르게 제공한다")
        @Test
        fun providesBrandInfo_withNameAndDescription() {
            // arrange
            val name = "테스트 브랜드"
            val description = "테스트 브랜드 설명"
            val brand = Brand.create(name = name, description = description)

            // act & assert
            assertAll(
                { assertThat(brand.name).isEqualTo(name) },
                { assertThat(brand.description).isEqualTo(description) },
            )
        }

        @DisplayName("브랜드 이름이 비어있으면 예외가 발생한다")
        @Test
        fun throwsException_whenNameIsBlank() {
            // act & assert
            assertThatThrownBy { Brand.create(name = "", description = "설명") }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("브랜드 설명이 비어있으면 예외가 발생한다")
        @Test
        fun throwsException_whenDescriptionIsBlank() {
            // act & assert
            assertThatThrownBy { Brand.create(name = "브랜드", description = "") }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("브랜드 이름이 공백만 있으면 예외가 발생한다")
        @Test
        fun throwsException_whenNameIsWhitespace() {
            // act & assert
            assertThatThrownBy { Brand.create(name = "   ", description = "설명") }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("브랜드 설명이 공백만 있으면 예외가 발생한다")
        @Test
        fun throwsException_whenDescriptionIsWhitespace() {
            // act & assert
            assertThatThrownBy { Brand.create(name = "브랜드", description = "   ") }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("BrandService")
    @Nested
    inner class BrandServiceTest {
        private val brandRepository: BrandRepository = mockk()
        private val brandService = BrandService(brandRepository)

        @DisplayName("활성 브랜드 정보를 조회한다")
        @Test
        fun getBrandInfo_success() {
            // arrange
            val brandName = "테스트 브랜드"
            val description = "테스트 브랜드 설명"
            val brand = Brand.create(name = brandName, description = description)

            every { brandRepository.findById(any()) } returns brand

            // act
            val result = brandService.getBrandInfo(1L)

            // assert
            assertAll(
                { assertThat(result.brandName).isEqualTo(brandName) },
                { assertThat(result.description).isEqualTo(description) },
            )
        }

        @DisplayName("삭제된 브랜드 조회 시 NOT_FOUND 예외를 던진다")
        @Test
        fun getBrandInfo_throwsException_whenBrandIsDeleted() {
            // arrange
            val deletedBrand = Brand.create(name = "삭제된 브랜드", description = "설명").apply { delete() }

            every { brandRepository.findById(any()) } returns deletedBrand

            // act & assert
            assertThatThrownBy { brandService.getBrandInfo(1L) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
        }

        @DisplayName("활성 브랜드 목록을 정확한 페이징 크기로 조회한다")
        @Test
        fun getAllBrands_success() {
            // arrange
            val brand1 = Brand.create(name = "브랜드1", description = "설명1")
            val brand2 = Brand.create(name = "브랜드2", description = "설명2")
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(listOf(brand1, brand2), pageable, 2)

            every { brandRepository.findAllActiveWithPage(any()) } returns page

            // act
            val result = brandService.getAllBrands(pageable)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.content[0].brandName).isEqualTo("브랜드1") },
                { assertThat(result.content[1].brandName).isEqualTo("브랜드2") },
                { assertThat(result.totalElements).isEqualTo(2L) },
                { assertThat(result.size).isEqualTo(20) },
            )
        }

        @DisplayName("빈 브랜드 목록을 페이징으로 조회할 수 있다")
        @Test
        fun getAllBrands_returnsEmpty() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl<Brand>(emptyList(), pageable, 0)

            every { brandRepository.findAllActiveWithPage(any()) } returns page

            // act
            val result = brandService.getAllBrands(pageable)

            // assert
            assertAll(
                { assertThat(result.content).isEmpty() },
                { assertThat(result.totalElements).isEqualTo(0L) },
            )
        }

        @DisplayName("새로운 브랜드를 등록할 수 있다")
        @Test
        fun createBrand_success() {
            // arrange
            val name = "새로운 브랜드"
            val description = "새 브랜드 설명"

            every { brandRepository.existsByName(any()) } returns false
            every { brandRepository.save(any()) } answers { firstArg() }

            // act
            val result = brandService.createBrand(name, description)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo(name) },
                { assertThat(result.description).isEqualTo(description) },
                { assertThat(result.isDeleted()).isFalse() },
            )
        }

        @DisplayName("이미 등록된 브랜드명으로는 등록할 수 없다")
        @Test
        fun createBrand_throwsException_whenNameAlreadyExists() {
            // arrange
            val name = "이미 등록된 브랜드"
            val description = "설명"

            every { brandRepository.existsByName(name) } returns true

            // act & assert
            assertThatThrownBy { brandService.createBrand(name, description) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("브랜드 정보를 수정할 수 있다")
        @Test
        fun updateBrand_success() {
            // arrange
            val originalBrand = Brand.create(name = "원본 브랜드", description = "원본 설명")
            val newName = "수정된 브랜드"
            val newDescription = "수정된 설명"

            every { brandRepository.findById(any()) } returns originalBrand
            every { brandRepository.save(any()) } answers { firstArg() }

            // act
            brandService.updateBrand(1L, newName, newDescription)

            // assert
            assertAll(
                { assertThat(originalBrand.name).isEqualTo(newName) },
                { assertThat(originalBrand.description).isEqualTo(newDescription) },
            )
        }

        @DisplayName("삭제된 브랜드는 수정할 수 없다")
        @Test
        fun updateBrand_throwsException_whenBrandIsDeleted() {
            // arrange
            val deletedBrand = Brand.create(name = "삭제된 브랜드", description = "설명").apply { delete() }

            every { brandRepository.findById(any()) } returns deletedBrand

            // act & assert
            assertThatThrownBy { brandService.updateBrand(1L, "새 이름", "새 설명") }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
        }

        @DisplayName("브랜드명이 비어있으면 수정할 수 없다")
        @Test
        fun updateBrand_throwsException_whenNameIsBlank() {
            // arrange
            val brand = Brand.create(name = "원본 브랜드", description = "원본 설명")
            every { brandRepository.findById(any()) } returns brand

            // act & assert
            assertThatThrownBy { brandService.updateBrand(1L, "", "설명") }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("브랜드 설명이 비어있으면 수정할 수 없다")
        @Test
        fun updateBrand_throwsException_whenDescriptionIsBlank() {
            // arrange
            val brand = Brand.create(name = "원본 브랜드", description = "원본 설명")
            every { brandRepository.findById(any()) } returns brand

            // act & assert
            assertThatThrownBy { brandService.updateBrand(1L, "브랜드", "") }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("브랜드를 삭제할 수 있다")
        @Test
        fun deleteBrand_success() {
            // arrange
            val brand = Brand.create(name = "삭제할 브랜드", description = "설명")

            every { brandRepository.findById(any()) } returns brand

            // act
            brandService.deleteBrand(1L)

            // assert
            assertThat(brand.isDeleted()).isTrue()
        }

        @DisplayName("이미 삭제된 브랜드를 다시 삭제할 수 없다")
        @Test
        fun deleteBrand_throwsException_whenAlreadyDeleted() {
            // arrange
            val deletedBrand = Brand.create(name = "이미 삭제된 브랜드", description = "설명").apply { delete() }

            every { brandRepository.findById(any()) } returns deletedBrand

            // act & assert
            assertThatThrownBy { brandService.deleteBrand(1L) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
        }
    }
}
