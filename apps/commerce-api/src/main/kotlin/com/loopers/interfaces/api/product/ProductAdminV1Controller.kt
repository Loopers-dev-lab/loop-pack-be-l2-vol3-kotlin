package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
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
class ProductAdminV1Controller(
    private val productFacade: ProductFacade,
) : ProductAdminV1ApiSpec {

    @GetMapping
    override fun getProducts(
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(defaultValue = "LATEST") sortType: ProductSortType,
        pageable: Pageable,
    ): ApiResponse<Page<ProductAdminV1Dto.ProductResponse>> =
        productFacade.getProducts(brandId, PageRequest.of(pageable.pageNumber, pageable.pageSize, sortType.sort))
            .map { ProductAdminV1Dto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }

    @GetMapping("/{productId}")
    override fun getProductById(@PathVariable productId: Long): ApiResponse<ProductAdminV1Dto.ProductResponse> =
        productFacade.getProductById(productId)
            .let { ProductAdminV1Dto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }

    @PostMapping
    override fun createProduct(
        @RequestBody request: ProductAdminV1Dto.CreateProductRequest,
    ): ApiResponse<ProductAdminV1Dto.ProductResponse> =
        productFacade.createProduct(
            brandId = request.brandId,
            name = request.name,
            imageUrl = request.imageUrl,
            description = request.description,
            price = request.price,
            quantity = request.quantity,
        ).let { ProductAdminV1Dto.ProductResponse.from(it) }
         .let { ApiResponse.success(it) }

    @PutMapping("/{productId}")
    override fun updateProduct(
        @PathVariable productId: Long,
        @RequestBody request: ProductAdminV1Dto.UpdateProductRequest,
    ): ApiResponse<ProductAdminV1Dto.ProductResponse> =
        productFacade.updateProduct(
            id = productId,
            name = request.name,
            imageUrl = request.imageUrl,
            description = request.description,
            price = request.price,
            quantity = request.quantity,
        ).let { ProductAdminV1Dto.ProductResponse.from(it) }
         .let { ApiResponse.success(it) }

    @DeleteMapping("/{productId}")
    override fun deleteProduct(@PathVariable productId: Long): ApiResponse<Unit> =
        productFacade.deleteProduct(productId).let { ApiResponse.success(Unit) }
}
