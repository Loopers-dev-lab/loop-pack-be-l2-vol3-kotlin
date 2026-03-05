package com.loopers.application.user.like

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.like.ProductLike
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import java.math.BigDecimal

@DisplayName("상품 좋아요 목록 조회")
class UserProductLikeListUseCaseTest {
    private val productLikeRepository: ProductLikeRepository = mock()
    private val productRepository: ProductRepository = mock()
    private val brandRepository: BrandRepository = mock()
    private val productStockRepository: ProductStockRepository = mock()
    private val useCase = UserProductLikeListUseCase(
        productLikeRepository,
        productRepository,
        brandRepository,
        productStockRepository,
    )

    private fun like(userId: Long, productId: Long): ProductLike =
        ProductLike.retrieve(id = productId, userId = userId, productId = productId)

    private fun activeProduct(id: Long, brandId: Long = 1L): Product =
        Product.retrieve(
            id = id,
            name = "상품$id",
            regularPrice = Money(BigDecimal("10000")),
            sellingPrice = Money(BigDecimal("8000")),
            brandId = brandId,
            imageUrl = null,
            thumbnailUrl = null,
            likeCount = 5,
            status = Product.Status.ACTIVE,
        )

    private fun inactiveProduct(id: Long, brandId: Long = 1L): Product =
        Product.retrieve(
            id = id,
            name = "상품$id",
            regularPrice = Money(BigDecimal("10000")),
            sellingPrice = Money(BigDecimal("8000")),
            brandId = brandId,
            imageUrl = null,
            thumbnailUrl = null,
            likeCount = 0,
            status = Product.Status.INACTIVE,
        )

    private fun activeBrand(id: Long, name: String = "브랜드$id"): Brand =
        Brand.retrieve(id = id, name = name, status = Brand.Status.ACTIVE)

    private fun inactiveBrand(id: Long): Brand =
        Brand.retrieve(id = id, name = "비활성 브랜드", status = Brand.Status.INACTIVE)

    private fun stock(productId: Long, quantity: Int): ProductStock =
        ProductStock.retrieve(id = productId, productId = productId, quantity = Quantity(quantity))

    private fun likePage(userId: Long, productIds: List<Long>): PageResponse<ProductLike> =
        PageResponse(
            content = productIds.map { like(userId, it) },
            totalElements = productIds.size.toLong(),
            page = 0,
            size = 20,
        )

    @Nested
    @DisplayName("ACTIVE 상품만 좋아요 목록에 포함된다")
    inner class WhenActiveProducts {
        @Test
        @DisplayName("정상 조회 — LikedProduct 필드가 올바르게 매핑된다")
        fun getList_success() {
            val pageRequest = PageRequest()
            given(productLikeRepository.findAllByUserId(eq(1L), eq(pageRequest)))
                .willReturn(likePage(1L, listOf(1L)))
            given(productRepository.findAllByIdIn(eq(listOf(1L))))
                .willReturn(listOf(activeProduct(1L, brandId = 10L)))
            given(brandRepository.findAllByIdIn(eq(listOf(10L))))
                .willReturn(listOf(activeBrand(10L, "나이키")))
            given(productStockRepository.findAllByProductIdIn(eq(listOf(1L))))
                .willReturn(listOf(stock(1L, 10)))

            val result = useCase.getList(1L, pageRequest)

            assertThat(result.content).hasSize(1)
            val item = result.content[0]
            assertThat(item.productId).isEqualTo(1L)
            assertThat(item.productName).isEqualTo("상품1")
            assertThat(item.sellingPrice).isEqualTo(BigDecimal("8000.00"))
            assertThat(item.brandId).isEqualTo(10L)
            assertThat(item.brandName).isEqualTo("나이키")
            assertThat(item.likeCount).isEqualTo(5)
            assertThat(item.soldOut).isFalse()
        }

        @Test
        @DisplayName("INACTIVE 상품은 목록에서 제외된다")
        fun getList_inactiveProductExcluded() {
            val pageRequest = PageRequest()
            given(productLikeRepository.findAllByUserId(eq(1L), eq(pageRequest)))
                .willReturn(likePage(1L, listOf(1L, 2L)))
            given(productRepository.findAllByIdIn(eq(listOf(1L, 2L))))
                .willReturn(listOf(activeProduct(1L), inactiveProduct(2L)))
            given(brandRepository.findAllByIdIn(eq(listOf(1L))))
                .willReturn(listOf(activeBrand(1L)))
            given(productStockRepository.findAllByProductIdIn(eq(listOf(1L, 2L))))
                .willReturn(listOf(stock(1L, 10)))

            val result = useCase.getList(1L, pageRequest)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].productId).isEqualTo(1L)
        }
    }

    @Nested
    @DisplayName("브랜드가 INACTIVE이면 좋아요 목록에서 제외된다")
    inner class WhenBrandInactive {
        @Test
        @DisplayName("INACTIVE 브랜드의 상품은 제외된다")
        fun getList_inactiveBrandExcluded() {
            val pageRequest = PageRequest()
            given(productLikeRepository.findAllByUserId(eq(1L), eq(pageRequest)))
                .willReturn(likePage(1L, listOf(1L, 2L)))
            given(productRepository.findAllByIdIn(eq(listOf(1L, 2L))))
                .willReturn(listOf(activeProduct(1L, brandId = 10L), activeProduct(2L, brandId = 20L)))
            given(brandRepository.findAllByIdIn(eq(listOf(10L, 20L))))
                .willReturn(listOf(activeBrand(10L), inactiveBrand(20L)))
            given(productStockRepository.findAllByProductIdIn(eq(listOf(1L, 2L))))
                .willReturn(listOf(stock(1L, 10), stock(2L, 5)))

            val result = useCase.getList(1L, pageRequest)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].productId).isEqualTo(1L)
        }
    }

    @Nested
    @DisplayName("품절 여부가 정확히 반영된다")
    inner class WhenSoldOut {
        @Test
        @DisplayName("재고 0 → soldOut=true")
        fun getList_soldOutTrue() {
            val pageRequest = PageRequest()
            given(productLikeRepository.findAllByUserId(eq(1L), eq(pageRequest)))
                .willReturn(likePage(1L, listOf(1L)))
            given(productRepository.findAllByIdIn(eq(listOf(1L))))
                .willReturn(listOf(activeProduct(1L)))
            given(brandRepository.findAllByIdIn(eq(listOf(1L))))
                .willReturn(listOf(activeBrand(1L)))
            given(productStockRepository.findAllByProductIdIn(eq(listOf(1L))))
                .willReturn(listOf(stock(1L, 0)))

            val result = useCase.getList(1L, pageRequest)

            assertThat(result.content[0].soldOut).isTrue()
        }

        @Test
        @DisplayName("재고 양수 → soldOut=false")
        fun getList_soldOutFalse() {
            val pageRequest = PageRequest()
            given(productLikeRepository.findAllByUserId(eq(1L), eq(pageRequest)))
                .willReturn(likePage(1L, listOf(1L)))
            given(productRepository.findAllByIdIn(eq(listOf(1L))))
                .willReturn(listOf(activeProduct(1L)))
            given(brandRepository.findAllByIdIn(eq(listOf(1L))))
                .willReturn(listOf(activeBrand(1L)))
            given(productStockRepository.findAllByProductIdIn(eq(listOf(1L))))
                .willReturn(listOf(stock(1L, 5)))

            val result = useCase.getList(1L, pageRequest)

            assertThat(result.content[0].soldOut).isFalse()
        }

        @Test
        @DisplayName("재고 레코드 없음 → soldOut=true")
        fun getList_stockMissing() {
            val pageRequest = PageRequest()
            given(productLikeRepository.findAllByUserId(eq(1L), eq(pageRequest)))
                .willReturn(likePage(1L, listOf(1L)))
            given(productRepository.findAllByIdIn(eq(listOf(1L))))
                .willReturn(listOf(activeProduct(1L)))
            given(brandRepository.findAllByIdIn(eq(listOf(1L))))
                .willReturn(listOf(activeBrand(1L)))
            given(productStockRepository.findAllByProductIdIn(eq(listOf(1L))))
                .willReturn(emptyList())

            val result = useCase.getList(1L, pageRequest)

            assertThat(result.content[0].soldOut).isTrue()
        }
    }
}
