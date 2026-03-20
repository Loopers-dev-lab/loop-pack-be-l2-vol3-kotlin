package com.loopers.application.auth

import com.loopers.application.error.ApplicationException
import com.loopers.application.member.MemberService
import com.loopers.domain.error.CoreException
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class AuthService(
    private val memberService: MemberService,
    private val cacheManager: CacheManager,
) {
    companion object {
        const val AUTH_CACHE = "auth-cache"
    }

    fun authenticate(loginId: String, password: String): AuthResult {
        val cache = cacheManager.getCache(AUTH_CACHE)
        val cachedAuth = cache?.get(loginId, CachedAuth::class.java)

        if (cachedAuth != null && cachedAuth.matchesPassword(password)) {
            return cachedAuth.toAuthResult()
        }

        val member = try {
            memberService.authenticate(loginId, password)
        } catch (e: CoreException) {
            throw ApplicationException.from(e)
        }

        val authResult = AuthResult(id = member.id, loginId = member.loginId)
        cache?.put(loginId, CachedAuth.of(authResult, password))

        return authResult
    }

    fun evictAuthCache(loginId: String) {
        cacheManager.getCache(AUTH_CACHE)?.evict(loginId)
    }
}
