package com.loopers.domain.event

import com.loopers.domain.event.model.EventHandled
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EventHandledRepositoryTest {

    private lateinit var repository: FakeEventHandledRepository

    @BeforeEach
    fun setUp() {
        repository = FakeEventHandledRepository()
    }

    @Nested
    @DisplayName("existsByEventId 시")
    inner class ExistsByEventId {

        @Test
        fun `저장된 eventId가 있으면 true를 반환한다`() {
            repository.save(EventHandled(eventId = "evt-1"))

            assertThat(repository.existsByEventId("evt-1")).isTrue()
        }

        @Test
        fun `저장된 eventId가 없으면 false를 반환한다`() {
            assertThat(repository.existsByEventId("evt-999")).isFalse()
        }
    }

    @Nested
    @DisplayName("save 시")
    inner class Save {

        @Test
        fun `EventHandled를 저장한다`() {
            val handled = EventHandled(eventId = "evt-1")

            val saved = repository.save(handled)

            assertThat(saved.eventId).isEqualTo("evt-1")
            assertThat(repository.existsByEventId("evt-1")).isTrue()
        }
    }
}
