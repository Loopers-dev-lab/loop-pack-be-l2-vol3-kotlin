package com.loopers.testcontainers

import com.redis.testcontainers.RedisContainer
import org.springframework.context.annotation.Configuration

@Configuration
class RedisTestContainersConfig {
    companion object {
        private val redisContainer = RedisContainer("redis:latest")
            .apply {
                start()
            }

        /** 컨테이너 호스트. `@DynamicPropertySource`에서 직접 참조해 사용한다. */
        val host: String
            get() = redisContainer.host

        val port: Int
            get() = redisContainer.firstMappedPort

        const val DATABASE: String = "0"
    }
}
