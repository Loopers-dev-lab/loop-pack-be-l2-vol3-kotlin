package com.loopers.interfaces.api.admin.product

import com.loopers.application.product.ProductFacade
import com.loopers.domain.product.ProductSort
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/products")
class AdminProductV1Controller(
    private val productFacade: ProductFacade,
) : AdminProductV1ApiSpec {
    @GetMapping
    override fun getProducts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) brandId: Long?,
    ): ApiResponse<PageResponse<AdminProductV1Dto.ProductResponse>> {
        return productFacade.getProducts(brandId, ProductSort.LATEST, PageRequest.of(page, size))
            .map { AdminProductV1Dto.ProductResponse.from(it) }
            .let { PageResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    override fun getProduct(@PathVariable productId: Long): ApiResponse<AdminProductV1Dto.ProductResponse> {
        return productFacade.getProduct(productId)
            .let { AdminProductV1Dto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping
    override fun createProduct(
        @RequestBody req: AdminProductV1Dto.CreateProductRequest,
    ): ApiResponse<AdminProductV1Dto.ProductResponse> {
        return productFacade.createProduct(req.brandId, req.name, req.description, req.price, req.stockQuantity)
            .let { AdminProductV1Dto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{productId}")
    override fun updateProduct(
        @PathVariable productId: Long,
        @RequestBody req: AdminProductV1Dto.UpdateProductRequest,
    ): ApiResponse<AdminProductV1Dto.ProductResponse> {
        return productFacade.updateProduct(productId, req.name, req.description, req.price, req.stockQuantity)
            .let { AdminProductV1Dto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{productId}")
    override fun deleteProduct(@PathVariable productId: Long): ApiResponse<Any> {
        productFacade.deleteProduct(productId)
        return ApiResponse.success()
    }
}
