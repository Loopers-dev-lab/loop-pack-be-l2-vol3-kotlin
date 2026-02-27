package com.loopers.application.product

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductInventoryService
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
class ProductFacadeIntegrationTest @Autowired constructor(
    private val productFacade: ProductFacade,
    private val brandService: BrandService,
    private val productInventoryService: ProductInventoryService,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand(
        name: String = "Nike",
        businessNumber: String = "123-45-67890",
    ) = brandService.createBrand(
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

    private fun createProduct(
        brandId: Long,
        name: String = "Air Max",
        quantity: Long = 100L,
    ) = productFacade.createProduct(
        brandId = brandId,
        name = name,
        imageUrl = "image.png",
        description = "설명",
        price = 50_000L,
        quantity = quantity,
    )

    @Nested
    inner class CreateProduct {

        @Test
        fun `브랜드 생성 후 상품과 재고 동시 생성 시 ProductInfo를 반환한다`() {
            val brand = createBrand()

            val result = productFacade.createProduct(
                brandId = brand.id,
                name = "Air Max",
                imageUrl = "image.png",
                description = "설명",
                price = 50_000L,
                quantity = 100L,
            )

            assertAll(
                { assertThat(result.id).isGreaterThan(0) },
                { assertThat(result.name).isEqualTo("Air Max") },
                { assertThat(result.price).isEqualTo(50_000L) },
                { assertThat(result.brand.id).isEqualTo(brand.id) },
                { assertThat(result.brand.name).isEqualTo("Nike") },
            )

            val inventory = productInventoryService.getInventory(result.id)
            assertThat(inventory.stock.value).isEqualTo(100L)
        }
    }

    @Nested
    inner class GetProducts {

        @Test
        fun `브랜드와 정렬 필터 조합으로 상품 목록을 조회한다`() {
            val brand1 = createBrand(name = "Nike", businessNumber = "123-45-00001")
            val brand2 = createBrand(name = "Adidas", businessNumber = "123-45-00002")
            createProduct(brand1.id, "Air Max")
            createProduct(brand1.id, "Air Force")
            createProduct(brand2.id, "Ultraboost")

            val resultByBrand = productFacade.getProducts(brand1.id, PageRequest.of(0, 10))
            assertThat(resultByBrand.content).hasSize(2)
            assertThat(resultByBrand.content.map { it.brand.id }).containsOnly(brand1.id)

            val resultAll = productFacade.getProducts(null, PageRequest.of(0, 10))
            assertThat(resultAll.content).hasSize(3)
        }
    }

    @Nested
    inner class GetProductById {

        @Test
        fun `단건 조회 시 BrandInfo가 포함된 ProductInfo를 반환한다`() {
            val brand = createBrand()
            val created = createProduct(brand.id)

            val result = productFacade.getProductById(created.id)

            assertThat(result.id).isEqualTo(created.id)
            assertThat(result.brand.id).isEqualTo(brand.id)
            assertThat(result.brand.name).isEqualTo("Nike")
        }

        @Test
        fun `존재하지 않는 ID 조회 시 NOT_FOUND 예외가 발생한다`() {
            val exception = assertThrows<CoreException> {
                productFacade.getProductById(99999L)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class UpdateProduct {

        @Test
        fun `상품과 재고 동시 수정 후 변경된 ProductInfo를 반환한다`() {
            val brand = createBrand()
            val created = createProduct(brand.id, quantity = 100L)

            val result = productFacade.updateProduct(
                id = created.id,
                name = "Air Max 2024",
                imageUrl = "new.png",
                description = "새 설명",
                price = 60_000L,
                quantity = 200L,
            )

            assertThat(result.name).isEqualTo("Air Max 2024")
            assertThat(result.price).isEqualTo(60_000L)
            assertThat(result.brand.id).isEqualTo(brand.id)

            val inventory = productInventoryService.getInventory(created.id)
            assertThat(inventory.stock.value).isEqualTo(200L)
        }
    }

    @Nested
    inner class DeleteProduct {

        @Test
        fun `상품과 재고 동시 삭제 후 조회 시 NOT_FOUND 예외가 발생한다`() {
            val brand = createBrand()
            val created = createProduct(brand.id)

            productFacade.deleteProduct(created.id)

            val exception = assertThrows<CoreException> {
                productFacade.getProductById(created.id)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
