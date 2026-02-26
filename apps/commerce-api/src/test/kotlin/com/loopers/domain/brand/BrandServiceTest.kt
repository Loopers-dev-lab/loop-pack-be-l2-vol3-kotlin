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
