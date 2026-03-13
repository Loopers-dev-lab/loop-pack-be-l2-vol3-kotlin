package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductFacade
import com.loopers.application.product.ProductInfo
import com.loopers.domain.product.DisplayStatus
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.SaleStatus
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
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
    private val productService: ProductService,
    private val productFacade: ProductFacade,
) {
    @GetMapping
    fun findAll(
        @RequestParam(required = false) brandId: Long?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<Page<ProductInfo>> {
        return ApiResponse.success(productFacade.getProductList(brandId, pageable))
    }

    @GetMapping("/{productId}")
    fun findById(@PathVariable productId: Long): ApiResponse<ProductInfo> {
        return ApiResponse.success(productFacade.getProductDetail(productId))
    }

    @PostMapping
    fun create(@RequestBody request: ProductAdminV1Dto.CreateRequest): ApiResponse<ProductAdminV1Dto.ProductResponse> {
        val product = productService.create(
            name = request.name,
            price = request.price,
            brandId = request.brandId,
            description = request.description,
            thumbnailImageUrl = request.thumbnailImageUrl,
            stockQuantity = request.stockQuantity,
        )
        return ApiResponse.success(ProductAdminV1Dto.ProductResponse.from(product))
    }

    @PutMapping("/{productId}")
    fun update(
        @PathVariable productId: Long,
        @RequestBody request: ProductAdminV1Dto.UpdateRequest,
    ): ApiResponse<ProductAdminV1Dto.ProductResponse> {
        val product = productService.update(
            id = productId,
            name = request.name,
            price = request.price,
            description = request.description,
            thumbnailImageUrl = request.thumbnailImageUrl,
            stockQuantity = request.stockQuantity,
            saleStatus = request.saleStatus,
            displayStatus = request.displayStatus,
        )
        return ApiResponse.success(ProductAdminV1Dto.ProductResponse.from(product))
    }

    @DeleteMapping("/{productId}")
    fun delete(@PathVariable productId: Long): ApiResponse<Any> {
        productService.delete(productId)
        return ApiResponse.success()
    }
}

class ProductAdminV1Dto {
    data class CreateRequest(
        val name: String,
        val price: Long,
        val brandId: Long,
        val description: String? = null,
        val thumbnailImageUrl: String? = null,
        val stockQuantity: Int = 0,
    )

    data class UpdateRequest(
        val name: String,
        val price: Long,
        val description: String? = null,
        val thumbnailImageUrl: String? = null,
        val stockQuantity: Int = 0,
        val saleStatus: SaleStatus = SaleStatus.SELLING,
        val displayStatus: DisplayStatus = DisplayStatus.VISIBLE,
    )

    data class ProductResponse(
        val id: Long,
        val name: String,
        val price: Long,
        val brandId: Long,
        val stockQuantity: Int,
        val saleStatus: SaleStatus,
        val displayStatus: DisplayStatus,
    ) {
        companion object {
            fun from(product: ProductModel): ProductResponse {
                return ProductResponse(
                    id = product.id,
                    name = product.name,
                    price = product.price,
                    brandId = product.brandId,
                    stockQuantity = product.stockQuantity,
                    saleStatus = product.saleStatus,
                    displayStatus = product.displayStatus,
                )
            }
        }
    }
}
