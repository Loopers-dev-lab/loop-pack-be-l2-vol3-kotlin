package com.loopers.interfaces.api.v1.product

import com.loopers.application.product.GetProductListUseCase
import com.loopers.application.product.GetProductUseCase
import com.loopers.application.product.ProductViewTracker
import com.loopers.application.ranking.GetRankingUseCase
import com.loopers.domain.product.ProductSortType
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val getProductUseCase: GetProductUseCase,
    private val getProductListUseCase: GetProductListUseCase,
    private val productViewTracker: ProductViewTracker,
    private val getRankingUseCase: GetRankingUseCase,
) {
    @GetMapping
    fun getProducts(
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(required = false, defaultValue = "CREATED_AT") sort: ProductSortType,
    ): ApiResponse<List<GetProductListResponse>> {
        val products = getProductListUseCase.getAllActive(brandId, sort)
        return ApiResponse.success(products.map { GetProductListResponse.from(it) })
    }

    @GetMapping("/{id}")
    fun getProduct(
        @PathVariable id: Long,
    ): ApiResponse<GetProductDetailResponse> {
        val productInfo = getProductUseCase.getActiveById(id)
        productViewTracker.track(id)
        val today = LocalDate.now().format(DATE_FORMATTER)
        val rankInfo = getRankingUseCase.getProductRank(today, id)
        return ApiResponse.success(GetProductDetailResponse.from(productInfo, rankInfo))
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
