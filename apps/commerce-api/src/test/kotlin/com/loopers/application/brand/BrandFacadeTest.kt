package com.loopers.application.brand

import com.loopers.domain.BaseEntity
import com.loopers.domain.brand.BrandService
import com.loopers.domain.brand.Address
import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BusinessNumber
import com.loopers.domain.brand.Description
import com.loopers.domain.brand.Email
import com.loopers.domain.brand.LogoImageUrl
import com.loopers.domain.brand.Name
import com.loopers.domain.brand.PhoneNumber
import com.loopers.domain.product.ProductInventoryService
import com.loopers.domain.product.ProductService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class BrandFacadeTest {

    @Mock
    private lateinit var brandService: BrandService

    @Mock
    private lateinit var productService: ProductService

    @Mock
    private lateinit var productInventoryService: ProductInventoryService

    @InjectMocks
    private lateinit var brandFacade: BrandFacade

    private fun createTestBrandModel(
        name: String = "Nike",
        logoImageUrl: String = "test.png",
        description: String = "테스트 브랜드",
        zipCode: String = "12345",
        roadAddress: String = "서울특별시 중구 테스트길 1",
        detailAddress: String = "1층",
        email: String = "nike@google.com",
        phoneNumber: String = "02-3783-4401",
        businessNumber: String = "123-45-67890",
    ): BrandModel {
        val model = BrandModel(
            name = Name(name),
            logoImageUrl = LogoImageUrl(logoImageUrl),
            description = Description(description),
            address = Address(zipCode, roadAddress, detailAddress),
            email = Email(email),
            phoneNumber = PhoneNumber(phoneNumber),
            businessNumber = BusinessNumber(businessNumber),
        )
        val now = ZonedDateTime.now()
        val createdAtField = BaseEntity::class.java.getDeclaredField("createdAt")
        createdAtField.isAccessible = true
        createdAtField.set(model, now)
        val updatedAtField = BaseEntity::class.java.getDeclaredField("updatedAt")
        updatedAtField.isAccessible = true
        updatedAtField.set(model, now)
        return model
    }

    @Nested
    inner class CreateBrand {

        @Test
        fun `브랜드 생성 시 BrandInfo로 변환하여 반환한다`() {
            val model = createTestBrandModel()
            whenever(brandService.createBrand(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(model)

            val result = brandFacade.createBrand(
                name = "Nike",
                logoImageUrl = "test.png",
                description = "테스트 브랜드",
                zipCode = "12345",
                roadAddress = "서울특별시 중구 테스트길 1",
                detailAddress = "1층",
                email = "nike@google.com",
                phoneNumber = "02-3783-4401",
                businessNumber = "123-45-67890",
            )

            assertThat(result).isInstanceOf(BrandInfo::class.java)
            assertThat(result.name).isEqualTo("Nike")
            assertThat(result.email).isEqualTo("nike@google.com")
            assertThat(result.businessNumber).isEqualTo("123-45-67890")
        }
    }

    @Nested
    inner class GetBrands {

        @Test
        fun `브랜드 목록 조회 시 Page of BrandInfo로 변환하여 반환한다`() {
            val pageable = PageRequest.of(0, 20)
            val models = listOf(
                createTestBrandModel(name = "Nike", businessNumber = "123-45-00001"),
                createTestBrandModel(name = "Adidas", businessNumber = "123-45-00002"),
            )
            whenever(brandService.getBrands(pageable)).thenReturn(PageImpl(models, pageable, models.size.toLong()))

            val result = brandFacade.getBrands(pageable)

            assertThat(result.content).hasSize(2)
            assertThat(result.content[0]).isInstanceOf(BrandInfo::class.java)
            assertThat(result.content.map { it.name }).containsExactly("Nike", "Adidas")
        }
    }

    @Nested
    inner class GetBrandById {

        @Test
        fun `단건 조회 시 BrandInfo로 변환하여 반환한다`() {
            val model = createTestBrandModel()
            whenever(brandService.getBrandById(any())).thenReturn(model)

            val result = brandFacade.getBrandById(1L)

            assertThat(result).isInstanceOf(BrandInfo::class.java)
            assertThat(result.name).isEqualTo("Nike")
        }
    }

    @Nested
    inner class UpdateBrand {

        @Test
        fun `브랜드 수정 시 변경된 BrandInfo를 반환한다`() {
            val model = createTestBrandModel(description = "변경된 설명")
            whenever(brandService.updateBrand(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(model)

            val result = brandFacade.updateBrand(
                id = 1L,
                name = "Nike",
                logoImageUrl = "test.png",
                description = "변경된 설명",
                zipCode = "12345",
                roadAddress = "서울특별시 중구 테스트길 1",
                detailAddress = "1층",
                email = "nike@google.com",
                phoneNumber = "02-3783-4401",
                businessNumber = "123-45-67890",
            )

            assertThat(result).isInstanceOf(BrandInfo::class.java)
            assertThat(result.description).isEqualTo("변경된 설명")
        }
    }

    @Nested
    inner class DeleteBrand {

        @Test
        fun `브랜드 삭제 시 productService와 productInventoryService가 호출된다`() {
            whenever(brandService.deleteBrand(any())).then { }
            whenever(productService.deleteAllByBrandId(any())).thenReturn(emptyList())

            val result = brandFacade.deleteBrand(1L)

            assertThat(result).isEqualTo(Unit)
        }
    }
}
