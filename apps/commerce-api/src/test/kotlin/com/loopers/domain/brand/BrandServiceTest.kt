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
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class BrandServiceTest {

    @Mock
    private lateinit var brandRepository: BrandRepository

    private lateinit var brandService: BrandService

    @BeforeEach
    fun setUp() {
        brandService = BrandService(brandRepository)
    }

    @DisplayName("브랜드 생성할 때,")
    @Nested
    inner class CreateBrand {

        @DisplayName("유효한 이름과 설명이 주어지면, 저장된 브랜드를 반환한다.")
        @Test
        fun returnsSavedBrand_whenValidNameAndDescriptionProvided() {
            // arrange
            val name = "나이키"
            val description = "스포츠 브랜드"
            val brand = Brand(name = name, description = description)

            whenever(brandRepository.save(any())).thenReturn(brand)

            // act
            val result = brandService.createBrand(name, description)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo(name) },
                { assertThat(result.description).isEqualTo(description) },
            )
        }

        @DisplayName("설명이 null이면, 설명 없이 저장된 브랜드를 반환한다.")
        @Test
        fun returnsSavedBrand_whenDescriptionIsNull() {
            // arrange
            val name = "무인양품"
            val brand = Brand(name = name, description = null)

            whenever(brandRepository.save(any())).thenReturn(brand)

            // act
            val result = brandService.createBrand(name, null)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo(name) },
                { assertThat(result.description).isNull() },
            )
        }
    }

    @DisplayName("브랜드 목록 조회할 때,")
    @Nested
    inner class GetBrands {

        @DisplayName("브랜드가 존재하면, 페이징된 브랜드 목록을 반환한다.")
        @Test
        fun returnsPagedBrands_whenBrandsExist() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val brands = listOf(
                Brand(name = "나이키", description = "스포츠 브랜드"),
                Brand(name = "아디다스", description = "스포츠 브랜드"),
            )
            val page: Page<Brand> = PageImpl(brands, pageable, brands.size.toLong())

            whenever(brandRepository.findAll(pageable)).thenReturn(page)

            // act
            val result = brandService.getBrands(pageable)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.content[0].name).isEqualTo("나이키") },
                { assertThat(result.content[1].name).isEqualTo("아디다스") },
                { assertThat(result.totalElements).isEqualTo(2) },
            )
        }

        @DisplayName("브랜드가 없으면, 빈 페이지를 반환한다.")
        @Test
        fun returnsEmptyPage_whenNoBrandsExist() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val page: Page<Brand> = PageImpl(emptyList(), pageable, 0)

            whenever(brandRepository.findAll(pageable)).thenReturn(page)

            // act
            val result = brandService.getBrands(pageable)

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

        @DisplayName("유효한 brandId를 전달하면, 브랜드를 반환한다.")
        @Test
        fun returnsBrand_whenValidBrandIdProvided() {
            // arrange
            val brandId = 1L
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")

            whenever(brandRepository.findById(brandId)).thenReturn(brand)

            // act
            val result = brandService.getBrand(brandId)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("나이키") },
                { assertThat(result.description).isEqualTo("스포츠 브랜드") },
            )
        }

        @DisplayName("존재하지 않는 brandId를 전달하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandNotExists() {
            // arrange
            val brandId = 999L

            whenever(brandRepository.findById(brandId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                brandService.getBrand(brandId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
