package com.loopers.application.auth

class FakeAuthCacheStore : AuthCacheStore {
    private val cache = mutableMapOf<String, CachedAuth>()

    override fun getAuth(loginId: String): CachedAuth? {
        return cache[loginId]
    }

    override fun putAuth(loginId: String, cachedAuth: CachedAuth) {
        cache[loginId] = cachedAuth
    }

    override fun evictAuth(loginId: String) {
        cache.remove(loginId)
    }

    fun clear() {
        cache.clear()
    }
}
