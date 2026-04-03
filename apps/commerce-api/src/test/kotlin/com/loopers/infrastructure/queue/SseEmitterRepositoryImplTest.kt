package com.loopers.infrastructure.queue

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class SseEmitterRepositoryImplTest {

    private lateinit var sseEmitterRepository: SseEmitterRepositoryImpl

    @BeforeEach
    fun setUp() {
        sseEmitterRepository = SseEmitterRepositoryImpl()
    }

    @Nested
    @DisplayName("add")
    inner class Add {

        @Test
        @DisplayName("userId로 SseEmitter를 저장하면, get으로 조회할 수 있다")
        fun `userId로 SseEmitter를 저장하면, get으로 조회할 수 있다`() {
            // given
            val userId = 1L
            val emitter = SseEmitter()

            // when
            sseEmitterRepository.add(userId, emitter)

            // then
            assertThat(sseEmitterRepository.get(userId)).isSameAs(emitter)
        }
    }

    @Nested
    @DisplayName("get")
    inner class Get {

        @Test
        @DisplayName("저장된 userId로 조회하면, 해당 SseEmitter를 반환한다")
        fun `저장된 userId로 조회하면, 해당 SseEmitter를 반환한다`() {
            // given
            val emitter1 = SseEmitter()
            val emitter2 = SseEmitter()
            sseEmitterRepository.add(1L, emitter1)
            sseEmitterRepository.add(2L, emitter2)

            // when
            val result = sseEmitterRepository.get(2L)

            // then
            assertThat(result).isSameAs(emitter2)
        }

        @Test
        @DisplayName("존재하지 않는 userId로 조회하면, null을 반환한다")
        fun `존재하지 않는 userId로 조회하면, null을 반환한다`() {
            // when
            val result = sseEmitterRepository.get(999L)

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("remove")
    inner class Remove {

        @Test
        @DisplayName("userId로 제거하면, 조회 시 null을 반환한다")
        fun `userId로 제거하면, 조회 시 null을 반환한다`() {
            // given
            val userId = 1L
            sseEmitterRepository.add(userId, SseEmitter())

            // when
            sseEmitterRepository.remove(userId)

            // then
            assertThat(sseEmitterRepository.get(userId)).isNull()
        }
    }
}
