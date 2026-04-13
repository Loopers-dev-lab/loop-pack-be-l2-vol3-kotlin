package com.loopers.infrastructure.ranking

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import java.time.LocalDate

/**
 * RedisRankingRepository.getTopN 의 Lua 결과 null 경로 단위 테스트.
 *
 * 실제 Redis 환경에서는 `ZREVRANGEBYSCORE`가 빈 키에도 빈 List를 반환하므로 null 경로가 재현되지 않는다.
 * null은 Spring Data Redis의 내부 직렬화/연결 이상 신호로 간주하고 장애로 승격시키는 것이 본 테스트의 목적.
 */
class RedisRankingRepositoryNullResultTest {

    /**
     * Mockito 금지 원칙에 따라 RedisTemplate을 직접 상속해
     * `execute(script, keys, args)` 한 메서드만 null 반환하도록 override한 수동 stub.
     *
     * Java 원본은 `<T> T execute(...)` 시그니처라 Kotlin에서 `T : Any` non-null로 보이지만,
     * 우리는 의도적으로 null을 흘려보내야 하므로 type parameter를 nullable로 더 느슨하게 잡는다.
     * 이 때 발생하는 covariance warning은 런타임 NPE보다 우선해 감수한다 — `: Any` 제약을
     * 그대로 두면 `null as T` 캐스팅이 런타임에 거부되어 stub 자체가 NPE를 던진다.
     */
    private class NullReturningRedisTemplate : RedisTemplate<String, String>() {
        @Suppress("RETURN_TYPE_MISMATCH_ON_OVERRIDE")
        override fun <T : Any?> execute(
            script: RedisScript<T>,
            keys: MutableList<String>,
            vararg args: Any?,
        ): T? = null
    }

    @Test
    @DisplayName("Lua 스크립트 결과가 null이면 정상 0건이 아닌 IllegalStateException을 던진다")
    fun `Lua null은 장애로 승격된다`() {
        // Arrange
        val repository = RedisRankingRepository(NullReturningRedisTemplate())

        // Act & Assert — "정상 0건"이라 은폐하지 말고 장애로 드러내야 한다
        assertThatThrownBy {
            repository.getTopN(date = LocalDate.of(2026, 4, 10), offset = 0, limit = 10)
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Redis Lua script returned null")
    }
}
