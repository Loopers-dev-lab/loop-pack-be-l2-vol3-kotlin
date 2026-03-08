package com.loopers.interfaces.api.product

import com.loopers.application.api.product.ProductFacade
import com.loopers.application.api.productlike.ProductLikeFacade
import com.loopers.domain.product.dto.ProductInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import com.loopers.support.validator.PageValidator
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
class ProductV1Controller(
    private val productFacade: ProductFacade,
    private val productLikeFacade: ProductLikeFacade,
) : ProductV1ApiSpec {

    @GetMapping("/{productId}")
    override fun getProductInfo(
        @PathVariable productId: Long,
    ): ApiResponse<ProductInfo> = ApiResponse.success(data = productFacade.getProductInfo(productId))

    @GetMapping
    override fun getProducts(
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) sort: String?,
    ): ApiResponse<PageResponse<ProductInfo>> {
        PageValidator.validatePageRequest(page, size)

        val sortOperation = ProductSortOption.fromValue(sort)
        val pageable = PageRequest.of(page, size, Sort.by(sortOperation.sortOrder))
        val pageData = productFacade.getActiveProducts(brandId, pageable)
        return ApiResponse.success(data = PageResponse.from(pageData))
    }

    @PostMapping("/{productId}/likes")
    override fun likeProduct(
        @RequestAttribute("userId") userId: Long,
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        productLikeFacade.likeProduct(userId, productId)
        return ApiResponse.success()
    }

    @DeleteMapping("/{productId}/likes")
    override fun unlikeProduct(
        @RequestAttribute("userId") userId: Long,
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        productLikeFacade.unlikeProduct(userId, productId)
        return ApiResponse.success()
    }
}
