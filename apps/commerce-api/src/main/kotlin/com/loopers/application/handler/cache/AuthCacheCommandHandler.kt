package com.loopers.application.handler.cache

import com.loopers.application.auth.AuthCacheStore
import com.loopers.domain.common.command.EvictAuthCacheCommand
import org.springframework.stereotype.Component

@Component
class AuthCacheCommandHandler(
    private val authCacheStore: AuthCacheStore,
) {
    fun handle(command: EvictAuthCacheCommand) {
        authCacheStore.evictAuth(command.loginId)
    }
}
