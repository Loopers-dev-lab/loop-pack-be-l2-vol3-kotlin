package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class AdminBrandFacadeTest {

    @Mock
    private lateinit var brandService: BrandService

    @Mock
    private lateinit var productService: ProductService

    private lateinit var adminBrandFacade: AdminBrandFacade

    @BeforeEach
    fun setUp() {
        adminBrandFacade = AdminBrandFacade(brandService, productService)
    }

    @DisplayName("브랜드 상세 조회할 때,")
    @Nested
    inner class GetBrand {

        @DisplayName("유효한 brandId를 전달하면, BrandInfo를 반환한다.")
        @Test
        fun returnsBrandInfo_whenValidBrandIdProvided() {
            // arrange
            val brandId = 1L
            val now = ZonedDateTime.now()
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")
            ReflectionTestUtils.setField(brand, "createdAt", now)
            ReflectionTestUtils.setField(brand, "updatedAt", now)

            whenever(brandService.getBrand(brandId)).thenReturn(brand)

            // act
            val result = adminBrandFacade.getBrand(brandId)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("나이키") },
                { assertThat(result.description).isEqualTo("스포츠 브랜드") },
                { assertThat(result.createdAt).isEqualTo(now) },
                { assertThat(result.updatedAt).isEqualTo(now) },
            )
        }
    }

    @DisplayName("브랜드 삭제할 때,")
    @Nested
    inner class DeleteBrand {

        @DisplayName("유효한 brandId를 전달하면, 브랜드를 삭제하고 소속 상품을 연쇄 삭제한다.")
        @Test
        fun deletesBrandAndCascadeDeletesProducts_whenValidBrandIdProvided() {
            // arrange
            val brandId = 1L
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")
            whenever(brandService.getBrand(brandId)).thenReturn(brand)

            // act
            adminBrandFacade.deleteBrand(brandId)

            // assert
            verify(brandService).delete(brand)
            verify(productService).deleteAllByBrandId(brandId)
        }
    }
}
