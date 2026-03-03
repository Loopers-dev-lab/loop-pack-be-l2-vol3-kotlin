package com.loopers.interfaces.api.admin.product

import com.loopers.application.admin.product.AdminProductDeleteUseCase
import com.loopers.application.admin.product.AdminProductDetailUseCase
import com.loopers.application.admin.product.AdminProductListUseCase
import com.loopers.application.admin.product.AdminProductRegisterUseCase
import com.loopers.application.admin.product.AdminProductUpdateUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api-admin/v1/products")
@RestController
class AdminProductV1Controller(
    private val registerUseCase: AdminProductRegisterUseCase,
    private val updateUseCase: AdminProductUpdateUseCase,
    private val deleteUseCase: AdminProductDeleteUseCase,
    private val detailUseCase: AdminProductDetailUseCase,
    private val listUseCase: AdminProductListUseCase,
) : AdminProductV1ApiSpec {

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    override fun register(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @Valid @RequestBody request: AdminProductV1Request.Register,
    ): ApiResponse<AdminProductV1Response.Register> {
        validateLdap(ldap)
        return registerUseCase.register(request.toCommand(ldap))
            .let { AdminProductV1Response.Register.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getList(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        pageRequest: PageRequest,
        brandId: Long?,
    ): ApiResponse<PageResponse<AdminProductV1Response.Summary>> {
        validateLdap(ldap)
        return listUseCase.getList(pageRequest, brandId)
            .map { AdminProductV1Response.Summary.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    override fun getDetail(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @PathVariable productId: Long,
    ): ApiResponse<AdminProductV1Response.Detail> {
        validateLdap(ldap)
        return detailUseCase.getDetail(productId)
            .let { AdminProductV1Response.Detail.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{productId}")
    override fun update(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @PathVariable productId: Long,
        @Valid @RequestBody request: AdminProductV1Request.Update,
    ): ApiResponse<AdminProductV1Response.Update> {
        validateLdap(ldap)
        return updateUseCase.update(request.toCommand(productId, ldap))
            .let { AdminProductV1Response.Update.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{productId}")
    override fun delete(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        validateLdap(ldap)
        deleteUseCase.delete(productId, ldap)
        return ApiResponse.success()
    }

    private fun validateLdap(ldap: String) {
        if (!LDAP_PATTERN.matches(ldap)) {
            throw CoreException(ErrorType.UNAUTHORIZED)
        }
    }

    companion object {
        private val LDAP_PATTERN = Regex("^loopers\\.[a-z]{1,12}$")
    }
}
