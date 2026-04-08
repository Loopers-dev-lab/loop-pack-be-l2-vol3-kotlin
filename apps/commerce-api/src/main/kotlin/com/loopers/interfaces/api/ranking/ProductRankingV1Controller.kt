package com.loopers.interfaces.api.ranking

import com.loopers.application.api.product.ProductRankingFacade
import com.loopers.domain.product.dto.ProductInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import com.loopers.support.validator.PageValidator
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/rankings")
class ProductRankingV1Controller(
    private val productRankingFacade: ProductRankingFacade,
) : ProductRankingV1ApiSpec {
    @GetMapping
    override fun getRankedProducts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false)
        @DateTimeFormat(pattern = "yyyyMMdd")
        date: LocalDate?,
    ): ApiResponse<PageResponse<ProductInfo>> {
        PageValidator.validatePageRequest(page, size)
        val pageData = productRankingFacade.getRankedProducts(date, page, size)
        return ApiResponse.success(data = PageResponse.from(pageData))
    }
}
