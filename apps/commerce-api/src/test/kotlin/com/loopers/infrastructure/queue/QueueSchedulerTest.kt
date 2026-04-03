package com.loopers.infrastructure.queue

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.script.RedisScript

@DisplayName("QueueScheduler")
class QueueSchedulerTest {
    private val redisTemplate: RedisTemplate<String, String> = mock()
    private val valueOperations: ValueOperations<String, String> = mock()
    private val scheduler = QueueScheduler(redisTemplate)

    init {
        given(redisTemplate.opsForValue()).willReturn(valueOperations)
        given(valueOperations.setIfAbsent(any(), any(), any())).willReturn(true)
    }

    @Nested
    @DisplayName("스케줄러가 대기열에서 유저를 꺼내 토큰을 발급한다")
    inner class ProcessQueue {
        @Test
        @DisplayName("대기열에 유저가 있으면 Lua 스크립트로 pop+issue 실행")
        fun schedule_issueTokens() {
            val luaResults = listOf("1", "token-1", "2", "token-2")
            given(redisTemplate.execute(any<RedisScript<List<*>>>(), any<List<String>>(), any()))
                .willReturn(luaResults)

            scheduler.processQueue()

            then(redisTemplate).should().execute(any<RedisScript<List<*>>>(), any<List<String>>(), any())
        }

        @Test
        @DisplayName("빈 대기열이면 빈 리스트 반환")
        fun schedule_emptyQueue() {
            given(redisTemplate.execute(any<RedisScript<List<*>>>(), any<List<String>>(), any()))
                .willReturn(emptyList<Any>())

            scheduler.processQueue()

            then(redisTemplate).should().execute(any<RedisScript<List<*>>>(), any<List<String>>(), any())
        }
    }

    @Nested
    @DisplayName("분산 락을 획득하지 못하면 skip한다")
    inner class LockNotAcquired {
        @Test
        @DisplayName("락 획득 실패 시 Lua 스크립트 실행 없음")
        fun schedule_lockNotAcquired() {
            given(valueOperations.setIfAbsent(any(), any(), any())).willReturn(false)

            scheduler.processQueue()

            then(redisTemplate).should(never()).execute(any<RedisScript<List<*>>>(), any<List<String>>(), any())
        }
    }
}
