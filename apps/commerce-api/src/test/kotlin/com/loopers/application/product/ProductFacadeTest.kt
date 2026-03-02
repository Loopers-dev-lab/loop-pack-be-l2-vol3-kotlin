package com.loopers.application.product

import com.loopers.application.brand.BrandInfo
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductInventoryService
import com.loopers.domain.product.ProductService
import com.loopers.domain.BaseEntity
import com.loopers.domain.brand.Address
import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BusinessNumber
import com.loopers.domain.brand.Description as BrandDescription
import com.loopers.domain.brand.Email as BrandEmail
import com.loopers.domain.brand.LogoImageUrl
import com.loopers.domain.brand.Name as BrandName
import com.loopers.domain.brand.PhoneNumber
import com.loopers.domain.product.Description
import com.loopers.domain.product.ImageUrl
import com.loopers.domain.product.Name
import com.loopers.domain.product.Price
import com.loopers.domain.product.ProductInventoryModel
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.Stock
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
class ProductFacadeTest {

    @Mock
    private lateinit var productService: ProductService

    @Mock
    private lateinit var productInventoryService: ProductInventoryService

    @Mock
    private lateinit var brandService: BrandService

    @InjectMocks
    private lateinit var productFacade: ProductFacade

    private fun createTestBrandModel(
        name: String = "Nike",
        businessNumber: String = "123-45-67890",
    ): BrandModel {
        val model = BrandModel(
            name = BrandName(name),
            logoImageUrl = LogoImageUrl("logo.png"),
            description = BrandDescription("테스트 브랜드"),
            address = Address("12345", "서울특별시 중구 테스트길 1", "1층"),
            email = BrandEmail("nike@google.com"),
            phoneNumber = PhoneNumber("02-3783-4401"),
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

    private fun createTestProductModel(
        brandId: Long = 1L,
        name: String = "뉴발란스 991",
        imageUrl: String = "test.png",
        description: String = "뉴발란스 신발",
        price: Long = 299_000L,
    ): ProductModel {
        val model = ProductModel(
            brandId = brandId,
            name = Name(name),
            imageUrl = ImageUrl(imageUrl),
            description = Description(description),
            price = Price(price),
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

    private fun createTestInventoryModel(productId: Long = 1L, stock: Long = 100L): ProductInventoryModel {
        return ProductInventoryModel(productId = productId, stock = Stock(stock))
    }

    @Nested
    inner class CreateProduct {

        @Test
        fun `상품 생성 시 BrandInfo가 포함된 ProductInfo를 반환한다`() {
            val productModel = createTestProductModel()
            val brandModel = createTestBrandModel()
            val inventoryModel = createTestInventoryModel()
            whenever(productService.createProduct(any(), any(), any(), any(), any())).thenReturn(productModel)
            whenever(productInventoryService.createInventory(any(), any())).thenReturn(inventoryModel)
            whenever(brandService.getBrandById(any())).thenReturn(brandModel)

            val result = productFacade.createProduct(
                brandId = 1L,
                name = "뉴발란스 991",
                imageUrl = "test.png",
                description = "뉴발란스 신발",
                price = 299_000L,
                quantity = 100L,
            )

            assertThat(result).isInstanceOf(ProductInfo::class.java)
            assertThat(result.name).isEqualTo("뉴발란스 991")
            assertThat(result.price).isEqualTo(299_000L)
            assertThat(result.brand).isInstanceOf(BrandInfo::class.java)
            assertThat(result.brand.name).isEqualTo("Nike")
        }
    }

    @Nested
    inner class GetProducts {

        @Test
        fun `상품 목록 조회 시 Page of ProductInfo로 변환하여 반환한다`() {
            val pageable = PageRequest.of(0, 20)
            val products = listOf(
                createTestProductModel(name = "뉴발란스 991"),
                createTestProductModel(name = "뉴발란스 992"),
            )
            val brandModel = createTestBrandModel()
            whenever(productService.getProducts(null, pageable)).thenReturn(PageImpl(products, pageable, products.size.toLong()))
            whenever(brandService.getBrandById(any())).thenReturn(brandModel)

            val result = productFacade.getProducts(null, pageable)

            assertThat(result.content).hasSize(2)
            assertThat(result.content[0]).isInstanceOf(ProductInfo::class.java)
            assertThat(result.content.map { it.name }).containsExactly("뉴발란스 991", "뉴발란스 992")
            assertThat(result.content[0].brand).isInstanceOf(BrandInfo::class.java)
        }
    }

    @Nested
    inner class GetProductById {

        @Test
        fun `단건 조회 시 BrandInfo가 포함된 ProductInfo를 반환한다`() {
            val productModel = createTestProductModel()
            val brandModel = createTestBrandModel()
            whenever(productService.getProductById(any())).thenReturn(productModel)
            whenever(brandService.getBrandById(any())).thenReturn(brandModel)

            val result = productFacade.getProductById(1L)

            assertThat(result).isInstanceOf(ProductInfo::class.java)
            assertThat(result.name).isEqualTo("뉴발란스 991")
            assertThat(result.brand).isInstanceOf(BrandInfo::class.java)
            assertThat(result.brand.name).isEqualTo("Nike")
        }
    }

    @Nested
    inner class UpdateProduct {

        @Test
        fun `상품 수정 시 변경된 ProductInfo를 BrandInfo와 함께 반환한다`() {
            val productModel = createTestProductModel(name = "뉴발란스 992", price = 350_000L)
            val brandModel = createTestBrandModel()
            val inventoryModel = createTestInventoryModel(stock = 200L)
            whenever(productService.updateProduct(any(), any(), any(), any(), any())).thenReturn(productModel)
            whenever(productInventoryService.updateStock(any(), any())).thenReturn(inventoryModel)
            whenever(brandService.getBrandById(any())).thenReturn(brandModel)

            val result = productFacade.updateProduct(
                id = 1L,
                name = "뉴발란스 992",
                imageUrl = "new.png",
                description = "새 신발",
                price = 350_000L,
                quantity = 200L,
            )

            assertThat(result).isInstanceOf(ProductInfo::class.java)
            assertThat(result.name).isEqualTo("뉴발란스 992")
            assertThat(result.price).isEqualTo(350_000L)
            assertThat(result.brand).isInstanceOf(BrandInfo::class.java)
        }
    }

    @Nested
    inner class DeleteProduct {

        @Test
        fun `상품 삭제 시 Unit을 반환한다`() {
            whenever(productService.deleteProduct(any())).then { }
            whenever(productInventoryService.deleteInventory(any())).then { }

            val result = productFacade.deleteProduct(1L)

            assertThat(result).isEqualTo(Unit)
        }
    }
}
