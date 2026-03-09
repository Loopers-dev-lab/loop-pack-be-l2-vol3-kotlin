package com.loopers.interfaces.api.catalog

import com.loopers.application.catalog.AdminGetProductUseCase
import com.loopers.application.catalog.AdminListProductsUseCase
import com.loopers.application.catalog.AdminRegisterProductUseCase
import com.loopers.application.catalog.ListProductsCriteria
import com.loopers.application.catalog.RegisterProductCriteria
import com.loopers.domain.catalog.ProductService
import com.loopers.domain.catalog.UpdateProductCommand
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/products")
class ProductV1AdminController(
    private val adminRegisterProductUseCase: AdminRegisterProductUseCase,
    private val adminGetProductUseCase: AdminGetProductUseCase,
    private val adminListProductsUseCase: AdminListProductsUseCase,
    private val productService: ProductService,
) : ProductV1AdminApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun register(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @RequestBody request: ProductV1AdminDto.RegisterRequest,
    ) {
        adminRegisterProductUseCase.execute(
            RegisterProductCriteria(
                brandId = request.brandId,
                name = request.name,
                quantity = request.quantity,
                price = request.price,
            ),
        )
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    override fun getProducts(
        @RequestHeader(value = "X-Loopers-Ldap") ldap: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) brandId: Long?,
    ): ApiResponse<ProductV1AdminDto.ProductSliceResponse> {
        return adminListProductsUseCase.execute(ListProductsCriteria(page = page, size = size, brandId = brandId))
            .let { ProductV1AdminDto.ProductSliceResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    @ResponseStatus(HttpStatus.OK)
    override fun getProduct(
        @RequestHeader(value = "X-Loopers-Ldap") ldap: String,
        @PathVariable productId: Long,
    ): ApiResponse<ProductV1AdminDto.ProductDetailResponse> {
        return adminGetProductUseCase.execute(productId)
            .let { ProductV1AdminDto.ProductDetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun modifyProduct(
        @RequestHeader(value = "X-Loopers-Ldap") ldap: String,
        @PathVariable productId: Long,
        @RequestBody request: ProductV1AdminDto.UpdateRequest,
    ) {
        productService.update(
            productId,
            UpdateProductCommand(
                newName = request.newName,
                newQuantity = request.newQuantity,
                newPrice = request.newPrice,
            ),
        )
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun deleteProduct(
        @RequestHeader(value = "X-Loopers-Ldap") ldap: String,
        @PathVariable productId: Long,
    ) {
        productService.delete(productId)
    }
}
