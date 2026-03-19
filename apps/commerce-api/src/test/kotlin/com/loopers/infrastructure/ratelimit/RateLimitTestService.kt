package com.loopers.infrastructure.ratelimit

import com.loopers.domain.ratelimit.RateLimit
import org.springframework.stereotype.Component

/**
 * @RateLimit AOP 테스트를 위한 서비스 빈.
 * AOP 프록시가 동작하려면 Spring 빈이어야 하므로 테스트 전용으로 생성.
 */
@Component
class RateLimitTestService {

    @RateLimit(
        key = "test-throw:{id}",
        ttl = 10,
        throwOnDuplicate = true,
        message = "테스트 중복 요청입니다.",
    )
    fun throwOnDuplicateMethod(id: Long): String {
        return "success"
    }

    @RateLimit(key = "test-silent:{id}", ttl = 10)
    fun silentMethod(id: Long): String {
        return "success"
    }
}
