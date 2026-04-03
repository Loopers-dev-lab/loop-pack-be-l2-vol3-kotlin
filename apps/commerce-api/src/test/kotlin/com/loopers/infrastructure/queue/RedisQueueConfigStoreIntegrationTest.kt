package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [RedisTestContainersConfig::class, RedisConfig::class, RedisCleanUp::class, RedisQueueConfigStoreImpl::class])
@DisplayName("RedisQueueConfigStoreImpl 통합 테스트")
class RedisQueueConfigStoreIntegrationTest @Autowired constructor(
    private val configStore: RedisQueueConfigStoreImpl,
    private val redisCleanUp: RedisCleanUp,
) {
    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("isEnabled / setEnabled")
    inner class EnabledToggle {

        @Test
        @DisplayName("기본 상태는 비활성(false)")
        fun `기본 비활성`() {
            assertThat(configStore.isEnabled()).isFalse()
        }

        @Test
        @DisplayName("활성화 후 true 반환")
        fun `활성화`() {
            configStore.setEnabled(true)

            assertThat(configStore.isEnabled()).isTrue()
        }

        @Test
        @DisplayName("비활성화 후 false 반환")
        fun `비활성화`() {
            configStore.setEnabled(true)
            configStore.setEnabled(false)

            assertThat(configStore.isEnabled()).isFalse()
        }
    }
}
