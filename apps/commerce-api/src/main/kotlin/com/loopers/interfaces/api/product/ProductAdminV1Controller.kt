package com.loopers.interfaces.api.product

import com.loopers.application.catalog.product.CreateProductUseCase
import com.loopers.application.catalog.product.DeleteProductUseCase
import com.loopers.application.catalog.product.GetProductUseCase
import com.loopers.application.catalog.product.GetProductsAdminUseCase
import com.loopers.application.catalog.product.RestoreProductUseCase
import com.loopers.application.catalog.product.UpdateProductUseCase
import com.loopers.interfaces.api.product.dto.ProductAdminV1Dto
import com.loopers.interfaces.api.product.spec.ProductAdminV1ApiSpec
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.toSpringPage
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api-admin/v1/products")
class ProductAdminV1Controller(
    private val createProductUseCase: CreateProductUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val restoreProductUseCase: RestoreProductUseCase,
    private val getProductUseCase: GetProductUseCase,
    private val getProductsAdminUseCase: GetProductsAdminUseCase,
) : ProductAdminV1ApiSpec {

    @GetMapping
    override fun getProducts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<ProductAdminV1Dto.AdminProductResponse>> {
        return getProductsAdminUseCase.execute(page, size)
            .map { ProductAdminV1Dto.AdminProductResponse.from(it) }
            .toSpringPage()
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    override fun getProduct(
        @PathVariable productId: Long,
    ): ApiResponse<ProductAdminV1Dto.AdminProductResponse> {
        return getProductUseCase.executeAdmin(productId)
            .let { ProductAdminV1Dto.AdminProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping
    override fun createProduct(
        @RequestBody request: ProductAdminV1Dto.CreateProductRequest,
    ): ApiResponse<ProductAdminV1Dto.AdminProductResponse> {
        return createProductUseCase.execute(
            brandId = request.brandId,
            name = request.name,
            price = request.price,
            stock = request.stock,
        )
            .let { ProductAdminV1Dto.AdminProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{productId}")
    override fun updateProduct(
        @PathVariable productId: Long,
        @RequestBody request: ProductAdminV1Dto.UpdateProductRequest,
    ): ApiResponse<ProductAdminV1Dto.AdminProductResponse> {
        return updateProductUseCase.execute(
            productId = productId,
            name = request.name,
            price = request.price,
            stock = request.stock,
            status = request.status,
        )
            .let { ProductAdminV1Dto.AdminProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{productId}")
    override fun deleteProduct(
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        deleteProductUseCase.execute(productId)
        return ApiResponse.success()
    }

    @PostMapping("/{productId}/restore")
    override fun restoreProduct(
        @PathVariable productId: Long,
    ): ApiResponse<ProductAdminV1Dto.AdminProductResponse> {
        return restoreProductUseCase.execute(productId)
            .let { ProductAdminV1Dto.AdminProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
