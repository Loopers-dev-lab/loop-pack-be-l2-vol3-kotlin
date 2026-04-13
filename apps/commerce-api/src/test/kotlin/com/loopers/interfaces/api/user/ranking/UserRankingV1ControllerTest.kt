package com.loopers.interfaces.api.user.ranking

import com.loopers.application.user.ranking.UserRankingListUseCase
import com.loopers.application.user.ranking.UserRankingResult
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("UserRankingV1Controller")
class UserRankingV1ControllerTest {
    private val listUseCase: UserRankingListUseCase = mock()
    private val controller = UserRankingV1Controller(listUseCase)

    @Nested
    @DisplayName("GET /api/v1/rankings — 랭킹 목록 조회")
    inner class GetList {

        @Test
        @DisplayName("date, page, size로 요청하면 랭킹 목록이 ApiResponse<PageResponse> 형태로 반환된다")
        fun getList_success() {
            val pageRequest = PageRequest().apply {
                page = 0
                size = 10
            }
            whenever(listUseCase.getList(any(), any())).thenReturn(
                PageResponse(
                    content = listOf(rankedProduct(rank = 1, score = 10.5, productId = 100)),
                    totalElements = 50,
                    page = 0,
                    size = 10,
                ),
            )

            val response = controller.getList("20260410", pageRequest)

            assertThat(response.meta.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS)
            assertThat(response.data?.content).hasSize(1)
            assertThat(response.data?.content?.first()?.rank).isEqualTo(1)
            assertThat(response.data?.content?.first()?.score).isEqualTo(10.5)
            assertThat(response.data?.content?.first()?.productId).isEqualTo(100)
            assertThat(response.data?.totalElements).isEqualTo(50)
        }

        @Test
        @DisplayName("date를 UseCase에 올바르게 전달한다")
        fun getList_parsesDate() {
            whenever(listUseCase.getList(any(), any())).thenReturn(emptyPage())

            controller.getList("20260410", PageRequest())

            verify(listUseCase).getList(
                check { date -> assertThat(date).isEqualTo(LocalDate.of(2026, 4, 10)) },
                any(),
            )
        }
    }

    @Nested
    @DisplayName("date 파라미터 미제공 시 오늘 날짜 기준으로 조회한다")
    inner class DefaultDate {

        @Test
        @DisplayName("date=null → 오늘 날짜로 UseCase 호출")
        fun getList_nullDate_usesToday() {
            whenever(listUseCase.getList(any(), any())).thenReturn(emptyPage())

            controller.getList(null, PageRequest())

            verify(listUseCase).getList(
                check { date -> assertThat(date).isEqualTo(LocalDate.now()) },
                any(),
            )
        }
    }

    @Nested
    @DisplayName("데이터 없는 날짜 조회 시 빈 목록이 반환된다")
    inner class EmptyResult {

        @Test
        @DisplayName("빈 결과 → content가 빈 리스트, totalElements=0")
        fun getList_emptyResult() {
            whenever(listUseCase.getList(any(), any())).thenReturn(emptyPage())

            val response = controller.getList("20260410", PageRequest())

            assertThat(response.data?.content).isEmpty()
            assertThat(response.data?.totalElements).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("date 형식이 yyyyMMdd가 아니면 BAD_REQUEST 예외를 던진다")
    inner class InvalidDateFormat {

        @Test
        @DisplayName("date=2026-04-10 (ISO 형식) → CoreException(BAD_REQUEST)")
        fun getList_isoFormat_throws() {
            assertThatThrownBy { controller.getList("2026-04-10", PageRequest()) }
                .isInstanceOf(CoreException::class.java)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("date=abc → CoreException(BAD_REQUEST)")
        fun getList_invalidString_throws() {
            assertThatThrownBy { controller.getList("abc", PageRequest()) }
                .isInstanceOf(CoreException::class.java)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    private fun rankedProduct(
        rank: Long,
        score: Double,
        productId: Long,
    ): UserRankingResult.RankedProduct =
        UserRankingResult.RankedProduct(
            rank = rank,
            score = score,
            productId = productId,
            productName = "상품$productId",
            sellingPrice = BigDecimal.valueOf(10000),
            thumbnailUrl = null,
        )

    private fun emptyPage(): PageResponse<UserRankingResult.RankedProduct> =
        PageResponse(emptyList(), 0, 0, 20)
}
