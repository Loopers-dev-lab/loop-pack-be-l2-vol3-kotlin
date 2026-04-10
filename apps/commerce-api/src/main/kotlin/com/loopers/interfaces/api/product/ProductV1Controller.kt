package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductService
import com.loopers.application.ranking.RankingService
import com.loopers.domain.product.ProductSortType
import com.loopers.interfaces.api.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/products")
class ProductV1Controller(
    private val productService: ProductService,
    private val rankingService: RankingService,
) : ProductV1ApiSpec {

    @GetMapping
    override fun getAllProducts(
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(required = false, defaultValue = "LATEST") sort: ProductSortType,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<Page<ProductV1Dto.ProductResponse>> {
        val sortedPageable = PageRequest.of(pageable.pageNumber, pageable.pageSize, sort.toSort())
        return productService.getAllProducts(brandId, sortedPageable)
            .map { ProductV1Dto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    override fun getProduct(
        @PathVariable productId: Long,
        @RequestHeader("X-Loopers-LoginId", required = false) loginId: String?,
        request: HttpServletRequest,
    ): ApiResponse<ProductV1Dto.ProductDetailResponse> {
        val clientIp = request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.remoteAddr
        val userAgent = request.getHeader("User-Agent")
        val referer = request.getHeader("Referer")

        val productInfo = productService.getProductInfo(productId, loginId, clientIp, userAgent, referer)
        val rank = rankingService.getProductRank(productId, LocalDate.now())
        return ApiResponse.success(ProductV1Dto.ProductDetailResponse.from(productInfo, rank))
    }
}
