package com.loopers.application.user.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import java.math.BigDecimal

@DisplayName("UserProductListUseCase")
class UserProductListUseCaseTest {
    private val productRepository: ProductRepository = mock()
    private val brandRepository: BrandRepository = mock()
    private val useCase = UserProductListUseCase(productRepository, brandRepository)

    private fun activeBrand(id: Long, name: String = "브랜드$id"): Brand =
        Brand.retrieve(id = id, name = name, status = Brand.Status.ACTIVE)

    private fun activeProduct(id: Long, brandId: Long, name: String = "상품$id"): Product =
        Product.retrieve(
            id = id,
            name = name,
            regularPrice = Money(BigDecimal("10000")),
            sellingPrice = Money(BigDecimal("8000")),
            brandId = brandId,
            imageUrl = null,
            thumbnailUrl = null,
            likeCount = 0,
            status = Product.Status.ACTIVE,
        )

    @Nested
    @DisplayName("목록 조회 결과를 응답으로 변환할 때")
    inner class WhenMapResult {
        @Test
        @DisplayName("repository에서 조회한 상품과 ACTIVE 브랜드를 매핑한다")
        fun getList_mapProductsWithBrands() {
            val product1 = activeProduct(1L, brandId = 1L)
            val product2 = activeProduct(2L, brandId = 2L)
            val page = PageResponse(
                content = listOf(product1, product2),
                totalElements = 2L,
                page = 0,
                size = 20,
            )
            given(productRepository.findAllActive(any(), isNull(), isNull())).willReturn(page)
            given(brandRepository.findAllByIdIn(eq(listOf(1L, 2L)))).willReturn(
                listOf(activeBrand(1L), activeBrand(2L)),
            )

            val result = useCase.getList(PageRequest(), null, null)

            assertThat(result.content).hasSize(2)
            assertThat(result.totalElements).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("잘못된 sort 값이 전달되면 BAD_REQUEST 예외를 던진다")
    inner class WhenInvalidSort {
        @Test
        @DisplayName("존재하지 않는 sort 값 → BAD_REQUEST")
        fun getList_invalidSort() {
            val exception = assertThrows<CoreException> {
                useCase.getList(PageRequest(), null, "invalid_sort")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("유효한 sort 값이 전달되면 정상 조회된다")
    inner class WhenValidSort {
        @Test
        @DisplayName("price_asc sort로 조회 성공")
        fun getList_validSort() {
            val page = PageResponse<Product>(
                content = emptyList(),
                totalElements = 0L,
                page = 0,
                size = 20,
            )
            given(productRepository.findAllActive(any(), isNull(), eq(Product.SortType.PRICE_ASC)))
                .willReturn(page)

            val result = useCase.getList(PageRequest(), null, "price_asc")

            assertThat(result.content).isEmpty()
        }

        @Test
        @DisplayName("likes_desc sort로 조회 성공")
        fun getList_validLikesSort() {
            val page = PageResponse<Product>(
                content = emptyList(),
                totalElements = 0L,
                page = 0,
                size = 20,
            )
            given(productRepository.findAllActive(any(), isNull(), eq(Product.SortType.LIKES_DESC)))
                .willReturn(page)

            val result = useCase.getList(PageRequest(), null, "likes_desc")

            assertThat(result.content).isEmpty()
        }
    }
}
