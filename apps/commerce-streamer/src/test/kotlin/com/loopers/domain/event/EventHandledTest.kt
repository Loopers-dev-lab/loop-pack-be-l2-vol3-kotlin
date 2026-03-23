package com.loopers.domain.event

import com.loopers.domain.event.model.EventHandled
import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EventHandledTest {

    @Nested
    @DisplayName("EventHandled 생성 시")
    inner class Create {

        @Test
        fun `eventId가 유효하면 생성된다`() {
            val handled = EventHandled(eventId = "evt-123")

            assertThat(handled.eventId).isEqualTo("evt-123")
            assertThat(handled.handledAt).isNotNull()
        }

        @Test
        fun `eventId가 빈 문자열이면 예외가 발생한다`() {
            assertThatThrownBy { EventHandled(eventId = "") }
                .isInstanceOf(CoreException::class.java)
        }

        @Test
        fun `eventId가 공백 문자열이면 예외가 발생한다`() {
            assertThatThrownBy { EventHandled(eventId = "   ") }
                .isInstanceOf(CoreException::class.java)
        }
    }
}
