package com.loopers.application.auth

interface AuthCacheStore {
    fun getAuth(loginId: String): CachedAuth?
    fun putAuth(loginId: String, cachedAuth: CachedAuth)
    fun evictAuth(loginId: String)
}
