package com.loopers.interfaces.api.auth

import com.loopers.support.error.CommonErrorCode
import com.loopers.support.error.CoreException
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AdminAuthArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AdminAuth::class.java) &&
            parameter.parameterType == Unit::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Unit {
        val ldapHeader = webRequest.getHeader(HEADER_LDAP)
        if (ldapHeader != ADMIN_LDAP_VALUE) {
            throw CoreException(CommonErrorCode.ADMIN_AUTHENTICATION_FAILED)
        }
    }

    companion object {
        private const val HEADER_LDAP = "X-Loopers-Ldap"
        private const val ADMIN_LDAP_VALUE = "loopers.admin"
    }
}
