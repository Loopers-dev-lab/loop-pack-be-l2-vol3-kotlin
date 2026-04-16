package com.loopers.interfaces.api.ranking

import com.loopers.domain.product.dto.ProductInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@Tag(name = "Product Ranking V1 API", description = "상품 랭킹 조회 API")
interface ProductRankingV1ApiSpec {
    @Operation(
        summary = "일간 상품 랭킹 조회",
        description = "오늘(또는 지정 날짜)의 상품 랭킹을 페이지 단위로 조회합니다",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 랭킹 조회 성공"),
        ],
    )
    @GetMapping("/daily")
    fun getDailyRankings(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "KST 기준 처리 일자 (yyyyMMdd, 미지정시 오늘)", required = false)
        @RequestParam(required = false) date: LocalDate?,
    ): ApiResponse<PageResponse<ProductInfo>>

    @Operation(
        summary = "주간 상품 랭킹 조회",
        description = "지정 날짜가 속한 주(월~일)의 상품 랭킹을 페이지 단위로 조회합니다",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 랭킹 조회 성공"),
        ],
    )
    @GetMapping("/weekly")
    fun getWeeklyRankings(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "KST 기준 처리 일자 (yyyyMMdd, 미지정시 오늘)", required = false)
        @RequestParam(required = false) date: LocalDate?,
    ): ApiResponse<PageResponse<ProductInfo>>

    @Operation(
        summary = "월간 상품 랭킹 조회",
        description = "지정 날짜가 속한 월의 상품 랭킹을 페이지 단위로 조회합니다",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 랭킹 조회 성공"),
        ],
    )
    @GetMapping("/monthly")
    fun getMonthlyRankings(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "KST 기준 처리 일자 (yyyyMMdd, 미지정시 오늘)", required = false)
        @RequestParam(required = false) date: LocalDate?,
    ): ApiResponse<PageResponse<ProductInfo>>
}
