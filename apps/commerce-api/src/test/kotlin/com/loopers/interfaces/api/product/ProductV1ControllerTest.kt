package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductFacade
import com.loopers.domain.product.ProductSortType
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

@DisplayName("ProductV1Controller")
class ProductV1ControllerTest {
    private val productFacade: ProductFacade = mockk()
    private val controller = ProductV1Controller(productFacade)

    @DisplayName("sort 파라미터를 우선 사용해 목록 조회를 위임한다")
    @Test
    fun delegatesUsingSortParameter() {
        // arrange
        val pageable = PageRequest.of(0, 20)
        every { productFacade.getProductList(1L, ProductSortType.LIKES_DESC, pageable) } returns PageImpl(emptyList(), pageable, 0)

        // act
        val response = controller.findAll(brandId = 1L, sort = "likes_desc", legacySort = null, pageable = pageable)

        // assert
        assertThat(response.meta.result.name).isEqualTo("SUCCESS")
        verify(exactly = 1) { productFacade.getProductList(1L, ProductSortType.LIKES_DESC, pageable) }
    }

    @DisplayName("legacy sortBy 파라미터도 호환 처리한다")
    @Test
    fun delegatesUsingLegacySortByParameter() {
        // arrange
        val pageable = PageRequest.of(0, 20)
        every { productFacade.getProductList(null, ProductSortType.PRICE_ASC, pageable) } returns PageImpl(emptyList(), pageable, 0)

        // act
        controller.findAll(brandId = null, sort = null, legacySort = "price_asc", pageable = pageable)

        // assert
        verify(exactly = 1) { productFacade.getProductList(null, ProductSortType.PRICE_ASC, pageable) }
    }

    @DisplayName("지원하지 않는 정렬값이면 BAD_REQUEST 예외가 발생한다")
    @Test
    fun throwsBadRequestWhenSortIsInvalid() {
        // arrange
        val pageable = PageRequest.of(0, 20)

        assertThatThrownBy {
            controller.findAll(brandId = null, sort = "random", legacySort = null, pageable = pageable)
        }
            .isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
    }
}
