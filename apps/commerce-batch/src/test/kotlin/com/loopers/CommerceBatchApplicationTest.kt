package com.loopers

import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.batch.job.enabled=false"])
class CommerceBatchApplicationTest {
    companion object {
        @Suppress("unused")
        private val mysqlTestContainersConfig = MySqlTestContainersConfig()

        @Suppress("unused")
        private val redisTestContainersConfig = RedisTestContainersConfig()

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("datasource.mysql-jpa.main.jdbc-url") {
                System.getProperty("datasource.mysql-jpa.main.jdbc-url")
            }
            registry.add("datasource.mysql-jpa.main.username") {
                System.getProperty("datasource.mysql-jpa.main.username")
            }
            registry.add("datasource.mysql-jpa.main.password") {
                System.getProperty("datasource.mysql-jpa.main.password")
            }
            registry.add("datasource.redis.database") {
                System.getProperty("datasource.redis.database")
            }
            registry.add("datasource.redis.master.host") {
                System.getProperty("datasource.redis.master.host")
            }
            registry.add("datasource.redis.master.port") {
                System.getProperty("datasource.redis.master.port")
            }
            registry.add("datasource.redis.replicas[0].host") {
                System.getProperty("datasource.redis.replicas[0].host")
            }
            registry.add("datasource.redis.replicas[0].port") {
                System.getProperty("datasource.redis.replicas[0].port")
            }
        }
    }

    @Test
    fun contextLoads() {}
}
