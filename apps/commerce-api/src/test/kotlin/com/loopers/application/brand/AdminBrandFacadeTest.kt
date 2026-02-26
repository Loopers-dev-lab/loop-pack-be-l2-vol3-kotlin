package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AdminBrandFacadeTest {

    @Mock
    private lateinit var brandService: BrandService

    private lateinit var adminBrandFacade: AdminBrandFacade

    @BeforeEach
    fun setUp() {
        adminBrandFacade = AdminBrandFacade(brandService)
    }

    @DisplayName("브랜드 상세 조회할 때,")
    @Nested
    inner class GetBrand {

        @DisplayName("유효한 brandId를 전달하면, BrandInfo를 반환한다.")
        @Test
        fun returnsBrandInfo_whenValidBrandIdProvided() {
            // arrange
            val brandId = 1L
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")

            whenever(brandService.getBrand(brandId)).thenReturn(brand)

            // act
            val result = adminBrandFacade.getBrand(brandId)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("나이키") },
                { assertThat(result.description).isEqualTo("스포츠 브랜드") },
            )
        }
    }
}
