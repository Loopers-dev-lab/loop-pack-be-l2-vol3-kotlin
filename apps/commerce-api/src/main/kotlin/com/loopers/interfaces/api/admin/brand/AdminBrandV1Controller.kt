package com.loopers.interfaces.api.admin.brand

import com.loopers.application.admin.brand.AdminBrandDeleteUseCase
import com.loopers.application.admin.brand.AdminBrandDetailUseCase
import com.loopers.application.admin.brand.AdminBrandListUseCase
import com.loopers.application.admin.brand.AdminBrandRegisterUseCase
import com.loopers.application.admin.brand.AdminBrandUpdateUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
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

@RequestMapping("/api-admin/v1/brands")
@RestController
class AdminBrandV1Controller(
    private val registerUseCase: AdminBrandRegisterUseCase,
    private val updateUseCase: AdminBrandUpdateUseCase,
    private val deleteUseCase: AdminBrandDeleteUseCase,
    private val detailUseCase: AdminBrandDetailUseCase,
    private val listUseCase: AdminBrandListUseCase,
) : AdminBrandV1ApiSpec {

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    override fun register(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @Valid @RequestBody request: AdminBrandV1Request.Register,
    ): ApiResponse<AdminBrandV1Response.Register> {
        validateLdap(ldap)
        return registerUseCase.register(request.toCommand(ldap))
            .let { AdminBrandV1Response.Register.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getList(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        pageRequest: PageRequest,
    ): ApiResponse<PageResponse<AdminBrandV1Response.Summary>> {
        validateLdap(ldap)
        return listUseCase.getList(pageRequest)
            .map { AdminBrandV1Response.Summary.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{brandId}")
    override fun getDetail(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @PathVariable brandId: Long,
    ): ApiResponse<AdminBrandV1Response.Detail> {
        validateLdap(ldap)
        return detailUseCase.getDetail(brandId)
            .let { AdminBrandV1Response.Detail.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{brandId}")
    override fun update(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @PathVariable brandId: Long,
        @Valid @RequestBody request: AdminBrandV1Request.Update,
    ): ApiResponse<AdminBrandV1Response.Update> {
        validateLdap(ldap)
        return updateUseCase.update(request.toCommand(brandId, ldap))
            .let { AdminBrandV1Response.Update.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{brandId}")
    override fun delete(
        @RequestHeader("X-Loopers-Ldap") ldap: String,
        @PathVariable brandId: Long,
    ): ApiResponse<Any> {
        validateLdap(ldap)
        deleteUseCase.delete(brandId, ldap)
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
