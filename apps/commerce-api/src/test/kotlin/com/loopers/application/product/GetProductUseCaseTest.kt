package com.loopers.application.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandName
import com.loopers.domain.brand.fixture.FakeBrandRepository
import com.loopers.domain.product.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductName
import com.loopers.domain.product.Stock
import com.loopers.domain.product.fixture.FakeProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetProductUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var getProductUseCase: GetProductUseCase

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        brandRepository = FakeBrandRepository()
        getProductUseCase = GetProductUseCase(productRepository, brandRepository)
    }

    @Test
    fun `정상 조회 시 brandName이 포함된 ProductInfo를 반환해야 한다`() {
        val brandId = brandRepository.save(createBrand())
        val productId = productRepository.save(createProduct(brandId))

        val result = getProductUseCase.getActiveById(productId)

        assertThat(result.id).isEqualTo(productId)
        assertThat(result.brandName).isEqualTo(BRAND_NAME)
        assertThat(result.brandLogoUrl).isEqualTo(BRAND_LOGO_URL)
    }

    @Test
    fun `존재하지 않는 상품 조회 시 CoreException이 발생해야 한다`() {
        assertThatThrownBy { getProductUseCase.getActiveById(999L) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND)
    }

    @Test
    fun `삭제된 상품 조회 시 CoreException이 발생해야 한다`() {
        val brandId = brandRepository.save(createBrand())
        val deletedProduct = createProduct(brandId).delete()
        val productId = productRepository.save(deletedProduct)

        assertThatThrownBy { getProductUseCase.getActiveById(productId) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND)
    }

    private fun createBrand() = Brand.create(
        name = BrandName(BRAND_NAME),
        description = BRAND_DESCRIPTION,
        logoUrl = BRAND_LOGO_URL,
    )

    private fun createProduct(brandId: Long) = Product.create(
        brandId = brandId,
        name = ProductName(PRODUCT_NAME),
        description = PRODUCT_DESCRIPTION,
        price = Money(PRICE),
        stock = Stock(STOCK),
        thumbnailUrl = THUMBNAIL_URL,
        images = emptyList(),
    )

    companion object {
        private const val BRAND_NAME = "테스트브랜드"
        private const val BRAND_DESCRIPTION = "브랜드 설명"
        private const val BRAND_LOGO_URL = "https://example.com/logo.png"
        private const val PRODUCT_NAME = "테스트상품"
        private const val PRODUCT_DESCRIPTION = "상품 설명"
        private const val PRICE = 10000L
        private const val STOCK = 100
        private const val THUMBNAIL_URL = "https://example.com/thumb.png"
    }
}
