package com.loopers.application.auth

import com.loopers.application.error.ApplicationException
import com.loopers.application.member.MemberService
import com.loopers.domain.error.CoreException
import org.springframework.stereotype.Component

@Component
class AuthService(
    private val memberService: MemberService,
    private val authCacheStore: AuthCacheStore,
) {
    fun authenticate(loginId: String, password: String): AuthResult {
        val cachedAuth = authCacheStore.getAuth(loginId)

        if (cachedAuth != null && cachedAuth.matchesPassword(password)) {
            return cachedAuth.toAuthResult()
        }

        val member = try {
            memberService.authenticate(loginId, password)
        } catch (e: CoreException) {
            throw ApplicationException.from(e)
        }

        val authResult = AuthResult(id = member.id, loginId = member.loginId)
        authCacheStore.putAuth(loginId, CachedAuth.of(authResult, password))

        return authResult
    }

    fun evictAuthCache(loginId: String) {
        authCacheStore.evictAuth(loginId)
    }
}
