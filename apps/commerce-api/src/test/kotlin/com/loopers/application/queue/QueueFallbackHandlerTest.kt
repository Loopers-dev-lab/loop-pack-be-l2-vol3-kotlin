package com.loopers.application.queue

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class QueueFallbackHandlerTest {

    private lateinit var handler: QueueFallbackHandler

    @BeforeEach
    fun setUp() {
        handler = QueueFallbackHandler()
    }

    @Nested
    @DisplayName("상태 전환 시")
    inner class StateTransition {

        @Test
        @DisplayName("초기 상태는 available이다")
        fun initialState_isAvailable() {
            assertThat(handler.isAvailable()).isTrue()
        }

        @Test
        @DisplayName("markUnavailable 호출 시 unavailable 상태로 전환한다")
        fun markUnavailable_changesState() {
            // act
            handler.markUnavailable()

            // assert
            assertThat(handler.isAvailable()).isFalse()
        }

        @Test
        @DisplayName("markAvailable 호출 시 available 상태로 복구한다")
        fun markAvailable_restoresState() {
            // arrange
            handler.markUnavailable()

            // act
            handler.markAvailable()

            // assert
            assertThat(handler.isAvailable()).isTrue()
        }

        @Test
        @DisplayName("이미 unavailable 상태에서 재호출해도 상태가 유지된다")
        fun markUnavailable_idempotent() {
            // act
            handler.markUnavailable()
            handler.markUnavailable()

            // assert
            assertThat(handler.isAvailable()).isFalse()
        }

        @Test
        @DisplayName("이미 available 상태에서 재호출해도 상태가 유지된다")
        fun markAvailable_idempotent() {
            // act
            handler.markAvailable()

            // assert
            assertThat(handler.isAvailable()).isTrue()
        }
    }
}
