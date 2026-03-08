package com.loopers.interfaces.admin.product

import com.loopers.application.admin.product.AdminProductFacade
import com.loopers.domain.product.dto.ProductInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import com.loopers.interfaces.admin.product.AdminProductV1Dto.CreateProductRequest
import com.loopers.interfaces.admin.product.AdminProductV1Dto.UpdateProductRequest
import com.loopers.support.validator.PageValidator
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/products")
class AdminProductV1Controller(
    private val adminProductFacade: AdminProductFacade,
) : AdminProductV1ApiSpec {

    @GetMapping("/{productId}")
    override fun getProductInfo(
        @PathVariable productId: Long,
    ): ApiResponse<ProductInfo> = ApiResponse.success(
        data = adminProductFacade.getProductInfo(productId),
    )

    @GetMapping
    override fun getProducts(
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) sort: String?,
    ): ApiResponse<PageResponse<ProductInfo>> {
        PageValidator.validatePageRequest(page, size)

        val sortOperation = AdminProductSortOption.fromValue(sort)
        val pageable = PageRequest.of(page, size, Sort.by(sortOperation.sortOrder))
        val pageData = adminProductFacade.getProducts(brandId, pageable)
        return ApiResponse.success(
            data = PageResponse.from(pageData),
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createProduct(
        @RequestBody @Valid request: CreateProductRequest,
    ): ApiResponse<Long> = ApiResponse.success(
        data = adminProductFacade.createProduct(
            brandId = request.brandId,
            name = request.name,
            price = request.price,
            status = request.status,
        ),
    )

    @PutMapping("/{productId}")
    override fun updateProduct(
        @PathVariable productId: Long,
        @RequestBody @Valid request: UpdateProductRequest,
    ): ApiResponse<Any> {
        adminProductFacade.updateProduct(
            id = productId,
            name = request.name,
            price = request.price,
            status = request.status,
        )
        return ApiResponse.success()
    }

    @DeleteMapping("/{productId}")
    override fun deleteProduct(
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        adminProductFacade.deleteProduct(productId)
        return ApiResponse.success()
    }
}
