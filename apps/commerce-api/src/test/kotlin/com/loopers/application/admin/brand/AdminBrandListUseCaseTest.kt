package com.loopers.application.admin.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

@DisplayName("AdminBrandListUseCase")
class AdminBrandListUseCaseTest {
    private val brandRepository: BrandRepository = mock()
    private val useCase = AdminBrandListUseCase(brandRepository)

    @Nested
    @DisplayName("브랜드 목록 조회 시")
    inner class WhenGetList {
        @Test
        @DisplayName("PageResponse<AdminBrandResult.Summary>를 반환한다")
        fun getList_success() {
            // arrange
            val pageRequest = PageRequest().apply {
                page = 0
                size = 20
            }
            val brands = listOf(
                Brand.retrieve(id = 1L, name = "나이키", status = Brand.Status.ACTIVE),
                Brand.retrieve(id = 2L, name = "아디다스", status = Brand.Status.INACTIVE),
            )
            given(brandRepository.findAll(eq(pageRequest))).willReturn(
                PageResponse(content = brands, totalElements = 2, page = 0, size = 20),
            )

            // act
            val result = useCase.getList(pageRequest)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].name).isEqualTo("나이키")
            assertThat(result.content[1].name).isEqualTo("아디다스")
            assertThat(result.totalElements).isEqualTo(2)
            assertThat(result.page).isEqualTo(0)
            assertThat(result.size).isEqualTo(20)
        }
    }
}
