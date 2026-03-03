package com.loopers.application.admin.product

import com.loopers.domain.common.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import java.math.BigDecimal

@DisplayName("AdminProductListUseCase")
class AdminProductListUseCaseTest {
    private val productRepository: ProductRepository = mock()
    private val useCase = AdminProductListUseCase(productRepository)

    private fun product(id: Long, brandId: Long = 1L): Product = Product.retrieve(
        id = id,
        name = "상품$id",
        regularPrice = Money(BigDecimal("10000")),
        sellingPrice = Money(BigDecimal("8000")),
        brandId = brandId,
        imageUrl = null,
        thumbnailUrl = null,
        likeCount = 0,
        status = Product.Status.ACTIVE,
    )

    @Nested
    @DisplayName("brandId 없이 전체 상품 목록을 조회한다")
    inner class WhenNoBrandFilter {
        @Test
        @DisplayName("전체 상품이 Summary로 반환된다")
        fun getList_all() {
            val page = PageResponse(
                content = listOf(product(1L), product(2L)),
                totalElements = 2L,
                page = 0,
                size = 20,
            )
            given(productRepository.findAll(org.mockito.kotlin.any(), isNull())).willReturn(page)

            val result = useCase.getList(PageRequest(), null)

            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].name).isEqualTo("상품1")
        }
    }

    @Nested
    @DisplayName("brandId로 필터링하여 상품 목록을 조회한다")
    inner class WhenBrandFilter {
        @Test
        @DisplayName("해당 브랜드의 상품만 반환된다")
        fun getList_byBrandId() {
            val page = PageResponse(
                content = listOf(product(1L, brandId = 5L)),
                totalElements = 1L,
                page = 0,
                size = 20,
            )
            given(productRepository.findAll(org.mockito.kotlin.any(), eq(5L))).willReturn(page)

            val result = useCase.getList(PageRequest(), 5L)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].brandId).isEqualTo(5L)
        }
    }
}
