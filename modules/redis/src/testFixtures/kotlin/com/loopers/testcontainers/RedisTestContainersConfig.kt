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

        // MySqlTestContainersConfig와 일관되게 class load 시점에 System property 설정.
        // `System.getProperty(...)`에 직접 의존하는 테스트(예: UserCouponIssueE2ETest)의
        // 하위 호환을 위해 유지.
        init {
            System.setProperty("datasource.redis.database", DATABASE)
            System.setProperty("datasource.redis.master.host", host)
            System.setProperty("datasource.redis.master.port", port.toString())
            System.setProperty("datasource.redis.replicas[0].host", host)
            System.setProperty("datasource.redis.replicas[0].port", port.toString())
        }
    }
}
