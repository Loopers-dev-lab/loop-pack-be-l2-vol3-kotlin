package com.loopers.application.user.product

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductQueryRepository
import com.loopers.domain.product.ProductQueryResult
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
    private val productQueryRepository: ProductQueryRepository = mock()
    private val useCase = UserProductListUseCase(productQueryRepository)

    @Nested
    @DisplayName("목록 조회 시")
    inner class WhenGetList {
        @Test
        @DisplayName("query repository 결과를 응답 DTO로 변환한다")
        fun getList_success() {
            given(productQueryRepository.getList(any(), isNull(), isNull())).willReturn(
                PageResponse(
                    content = listOf(
                        ProductQueryResult.Summary(
                            id = 1L,
                            name = "캐시상품",
                            sellingPrice = BigDecimal("8000"),
                            brandId = 1L,
                            brandName = "브랜드1",
                            thumbnailUrl = null,
                            likeCount = 3,
                        ),
                    ),
                    totalElements = 1L,
                    page = 0,
                    size = 20,
                ),
            )

            val result = useCase.getList(PageRequest(), null, null)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("캐시상품")
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
    @DisplayName("유효한 sort 값이 전달되면 query repository를 호출한다")
    inner class WhenValidSort {
        @Test
        @DisplayName("price_asc sort로 조회 성공")
        fun getList_validSort() {
            given(productQueryRepository.getList(any(), isNull(), eq(Product.SortType.PRICE_ASC)))
                .willReturn(PageResponse<ProductQueryResult.Summary>(emptyList(), 0L, 0, 20))

            val result = useCase.getList(PageRequest(), null, "price_asc")

            assertThat(result.content).isEmpty()
        }

        @Test
        @DisplayName("likes_desc sort로 조회 성공")
        fun getList_validLikesSort() {
            given(productQueryRepository.getList(any(), isNull(), eq(Product.SortType.LIKES_DESC)))
                .willReturn(PageResponse<ProductQueryResult.Summary>(emptyList(), 0L, 0, 20))

            val result = useCase.getList(PageRequest(), null, "likes_desc")

            assertThat(result.content).isEmpty()
        }
    }
}
