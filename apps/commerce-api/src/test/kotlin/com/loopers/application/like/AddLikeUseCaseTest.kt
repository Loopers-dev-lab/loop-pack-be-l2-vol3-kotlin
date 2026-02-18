package com.loopers.application.like

import com.loopers.domain.like.fixture.FakeLikeRepository
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

class AddLikeUseCaseTest {

    private lateinit var likeRepository: FakeLikeRepository
    private lateinit var productRepository: FakeProductRepository
    private lateinit var addLikeUseCase: AddLikeUseCase

    @BeforeEach
    fun setUp() {
        likeRepository = FakeLikeRepository()
        productRepository = FakeProductRepository()
        addLikeUseCase = AddLikeUseCase(likeRepository, productRepository)
    }

    @Test
    fun `정상 좋아요 추가 시 likeCount가 증가해야 한다`() {
        val productId = productRepository.save(createProduct())

        addLikeUseCase.add(USER_ID, productId)

        val product = productRepository.findById(productId)!!
        assertThat(product.likeCount).isEqualTo(1)
    }

    @Test
    fun `이미 좋아요한 상품에 다시 좋아요하면 likeCount가 변하지 않아야 한다`() {
        val productId = productRepository.save(createProduct())
        addLikeUseCase.add(USER_ID, productId)

        addLikeUseCase.add(USER_ID, productId)

        val product = productRepository.findById(productId)!!
        assertThat(product.likeCount).isEqualTo(1)
    }

    @Test
    fun `삭제된 상품에 좋아요 시 CoreException이 발생해야 한다`() {
        val deletedProduct = createProduct().delete()
        val productId = productRepository.save(deletedProduct)

        assertThatThrownBy { addLikeUseCase.add(USER_ID, productId) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND)
    }

    @Test
    fun `존재하지 않는 상품에 좋아요 시 CoreException이 발생해야 한다`() {
        assertThatThrownBy { addLikeUseCase.add(USER_ID, 999L) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND)
    }

    private fun createProduct() = Product.create(
        brandId = BRAND_ID,
        name = ProductName(PRODUCT_NAME),
        description = PRODUCT_DESCRIPTION,
        price = Money(PRICE),
        stock = Stock(STOCK),
        thumbnailUrl = THUMBNAIL_URL,
        images = emptyList(),
    )

    companion object {
        private const val USER_ID = 1L
        private const val BRAND_ID = 1L
        private const val PRODUCT_NAME = "테스트상품"
        private const val PRODUCT_DESCRIPTION = "상품 설명"
        private const val PRICE = 10000L
        private const val STOCK = 100
        private const val THUMBNAIL_URL = "https://example.com/thumb.png"
    }
}
