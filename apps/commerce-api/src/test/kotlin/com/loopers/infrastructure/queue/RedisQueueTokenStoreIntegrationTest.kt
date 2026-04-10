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

@SpringBootTest(classes = [RedisTestContainersConfig::class, RedisConfig::class, RedisCleanUp::class, RedisQueueTokenStoreImpl::class])
@DisplayName("RedisQueueTokenStoreImpl 통합 테스트")
class RedisQueueTokenStoreIntegrationTest @Autowired constructor(
    private val tokenStore: RedisQueueTokenStoreImpl,
    private val redisCleanUp: RedisCleanUp,
) {
    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("issue & get")
    inner class IssueAndGet {

        @Test
        @DisplayName("토큰을 발급하고 조회할 수 있다")
        fun `토큰 발급 및 조회`() {
            val issued = tokenStore.issue(1L, "token-abc", 300)

            assertThat(issued).isTrue()
            assertThat(tokenStore.get(1L)).isEqualTo("token-abc")
        }

        @Test
        @DisplayName("이미 토큰이 있는 멤버에게는 중복 발급되지 않는다")
        fun `중복 발급 방지`() {
            tokenStore.issue(1L, "token-first", 300)

            val result = tokenStore.issue(1L, "token-second", 300)

            assertThat(result).isFalse()
            assertThat(tokenStore.get(1L)).isEqualTo("token-first")
        }

        @Test
        @DisplayName("존재하지 않는 멤버 토큰 조회 시 null")
        fun `없는 토큰 조회`() {
            assertThat(tokenStore.get(999L)).isNull()
        }
    }

    @Nested
    @DisplayName("delete")
    inner class Delete {

        @Test
        @DisplayName("토큰을 삭제하면 조회 시 null")
        fun `토큰 삭제`() {
            tokenStore.issue(1L, "token-abc", 300)

            val deleted = tokenStore.delete(1L)

            assertThat(deleted).isTrue()
            assertThat(tokenStore.get(1L)).isNull()
        }

        @Test
        @DisplayName("없는 토큰 삭제 시 false")
        fun `없는 토큰 삭제`() {
            assertThat(tokenStore.delete(999L)).isFalse()
        }
    }

    @Nested
    @DisplayName("activeCount")
    inner class ActiveCount {

        @Test
        @DisplayName("활성 토큰 수를 반환한다")
        fun `활성 토큰 수`() {
            tokenStore.issue(1L, "t1", 300)
            tokenStore.issue(2L, "t2", 300)
            tokenStore.issue(3L, "t3", 300)

            assertThat(tokenStore.activeCount()).isEqualTo(3)
        }

        @Test
        @DisplayName("토큰이 없으면 0")
        fun `토큰 없음`() {
            assertThat(tokenStore.activeCount()).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("TTL 만료")
    inner class TtlExpiry {

        @Test
        @DisplayName("TTL 1초 토큰은 만료 후 조회 불가")
        fun `토큰 TTL 만료`() {
            tokenStore.issue(1L, "token-short", 1)

            Thread.sleep(1500)

            assertThat(tokenStore.get(1L)).isNull()
        }
    }
}
