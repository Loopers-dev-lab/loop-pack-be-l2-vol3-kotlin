package com.loopers.interfaces.support.auth

import com.loopers.interfaces.support.ATTRIBUTE_USER_ID
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import kotlin.reflect.jvm.kotlinFunction

@Component
class AuthUserArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthUser::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Long? {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
        val userId = request?.getAttribute(ATTRIBUTE_USER_ID) as? Long

        if (userId == null && !isNullable(parameter)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")
        }

        return userId
    }

    private fun isNullable(parameter: MethodParameter): Boolean {
        val kFunction = parameter.method?.kotlinFunction ?: return false
        val kParameter = kFunction.parameters.getOrNull(parameter.parameterIndex + 1) ?: return false
        return kParameter.type.isMarkedNullable
    }
}
