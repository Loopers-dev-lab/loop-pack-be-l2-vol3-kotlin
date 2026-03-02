package com.loopers.application.brand

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest

@SpringBootTest
class BrandFacadeIntegrationTest @Autowired constructor(
    private val brandFacade: BrandFacade,
    private val brandService: BrandService,
    private val productService: ProductService,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand(
        name: String = "Nike",
        businessNumber: String = "123-45-67890",
    ) = brandFacade.createBrand(
        name = name,
        logoImageUrl = "test.png",
        description = "테스트 브랜드",
        zipCode = "12345",
        roadAddress = "서울특별시 중구 테스트길 1",
        detailAddress = "1층",
        email = "nike@google.com",
        phoneNumber = "02-3783-4401",
        businessNumber = businessNumber,
    )

    @Nested
    inner class CreateBrand {

        @Test
        fun `브랜드 생성 시 BrandInfo를 반환한다 (businessNumber 포함)`() {
            val result = createBrand()

            assertAll(
                { assertThat(result.id).isGreaterThan(0) },
                { assertThat(result.name).isEqualTo("Nike") },
                { assertThat(result.logoImageUrl).isEqualTo("test.png") },
                { assertThat(result.businessNumber).isEqualTo("123-45-67890") },
                { assertThat(result.zipCode).isEqualTo("12345") },
            )
        }
    }

    @Nested
    inner class GetBrands {

        @Test
        fun `페이징 BrandInfo 목록을 반환한다`() {
            createBrand(name = "Nike", businessNumber = "123-45-00001")
            createBrand(name = "Adidas", businessNumber = "123-45-00002")
            createBrand(name = "NewBalance", businessNumber = "123-45-00003")

            val result = brandFacade.getBrands(PageRequest.of(0, 10))

            assertThat(result.content).hasSize(3)
            assertThat(result.content.map { it.name }).containsExactlyInAnyOrder("Nike", "Adidas", "NewBalance")
        }
    }

    @Nested
    inner class GetBrandById {

        @Test
        fun `BrandInfo 단건 조회 시 모든 필드를 반환한다`() {
            val created = createBrand()

            val result = brandFacade.getBrandById(created.id)

            assertThat(result.id).isEqualTo(created.id)
            assertThat(result.name).isEqualTo("Nike")
            assertThat(result.businessNumber).isEqualTo("123-45-67890")
        }

        @Test
        fun `존재하지 않는 ID 조회 시 NOT_FOUND 예외가 발생한다`() {
            val exception = assertThrows<CoreException> {
                brandFacade.getBrandById(99999L)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class UpdateBrand {

        @Test
        fun `브랜드 수정 후 수정된 BrandInfo를 반환한다`() {
            val created = createBrand()

            val result = brandFacade.updateBrand(
                id = created.id,
                name = "Nike Updated",
                logoImageUrl = "new_logo.png",
                description = "업데이트된 설명",
                zipCode = "54321",
                roadAddress = "서울특별시 강남구 새로운길 1",
                detailAddress = "2층",
                email = "updated@google.com",
                phoneNumber = "02-9876-5432",
                businessNumber = "123-45-67890",
            )

            assertThat(result.name).isEqualTo("Nike Updated")
            assertThat(result.description).isEqualTo("업데이트된 설명")
            assertThat(result.logoImageUrl).isEqualTo("new_logo.png")
        }
    }

    @Nested
    inner class DeleteBrand {

        @Test
        fun `브랜드와 소속 상품 및 재고 모두 soft delete 된다`() {
            val brand = brandService.createBrand(
                name = "Nike",
                logoImageUrl = "test.png",
                description = "테스트",
                zipCode = "12345",
                roadAddress = "서울특별시 중구 테스트길 1",
                detailAddress = "1층",
                email = "nike@google.com",
                phoneNumber = "02-3783-4401",
                businessNumber = "123-45-67890",
            )
            productService.createProduct(brand.id, "Air Max", "img1.png", "설명1", 50_000L)
            productService.createProduct(brand.id, "Air Force", "img2.png", "설명2", 30_000L)

            brandFacade.deleteBrand(brand.id)

            val brandException = assertThrows<CoreException> {
                brandFacade.getBrandById(brand.id)
            }
            assertThat(brandException.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
