package com.loopers.application.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandException
import com.loopers.domain.brand.BrandName
import com.loopers.domain.brand.fixture.FakeBrandRepository
import com.loopers.domain.product.fixture.FakeProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RegisterProductUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var registerProductUseCase: RegisterProductUseCase

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        brandRepository = FakeBrandRepository()
        registerProductUseCase = RegisterProductUseCase(productRepository, brandRepository)
    }

    @Test
    fun `정상 요청의 경우 상품이 등록되고 ID를 반환해야 한다`() {
        val brandId = brandRepository.save(createBrand())
        val command = createCommand(brandId)

        val result = registerProductUseCase.register(command)

        assertThat(result).isPositive()
    }

    @Test
    fun `브랜드가 존재하지 않으면 CoreException이 발생해야 한다`() {
        val command = createCommand(brandId = 999L)

        assertThatThrownBy { registerProductUseCase.register(command) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND)
    }

    @Test
    fun `삭제된 브랜드인 경우 BrandException이 발생해야 한다`() {
        val deletedBrand = createBrand().delete()
        val brandId = brandRepository.save(deletedBrand)
        val command = createCommand(brandId)

        assertThatThrownBy { registerProductUseCase.register(command) }
            .isInstanceOf(BrandException::class.java)
    }

    private fun createBrand() = Brand.create(
        name = BrandName(BRAND_NAME),
        description = BRAND_DESCRIPTION,
        logoUrl = BRAND_LOGO_URL,
    )

    private fun createCommand(brandId: Long) = RegisterProductCommand(
        brandId = brandId,
        name = PRODUCT_NAME,
        description = PRODUCT_DESCRIPTION,
        price = PRICE,
        stock = STOCK,
        thumbnailUrl = THUMBNAIL_URL,
        images = listOf(
            ProductImageCommand(imageUrl = IMAGE_URL, displayOrder = 0),
        ),
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
        private const val IMAGE_URL = "https://example.com/image.png"
    }
}
