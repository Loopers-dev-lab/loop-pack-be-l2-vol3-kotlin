package com.loopers.application.event

import com.loopers.domain.event.EventHandledRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class IdempotencyServiceTest {

    @Mock
    private lateinit var eventHandledRepository: EventHandledRepository

    @InjectMocks
    private lateinit var idempotencyService: IdempotencyService

    @DisplayName("이벤트 중복 여부를 확인할 때,")
    @Nested
    inner class IsAlreadyHandled {

        @DisplayName("이미 처리된 이벤트이면, true를 반환한다.")
        @Test
        fun returnsTrue_whenAlreadyHandled() {
            // arrange
            whenever(eventHandledRepository.existsByEventId("event-1")).thenReturn(true)

            // act
            val result = idempotencyService.isAlreadyHandled("event-1")

            // assert
            assertThat(result).isTrue()
        }

        @DisplayName("처리되지 않은 이벤트이면, false를 반환한다.")
        @Test
        fun returnsFalse_whenNotHandled() {
            // arrange
            whenever(eventHandledRepository.existsByEventId("event-1")).thenReturn(false)

            // act
            val result = idempotencyService.isAlreadyHandled("event-1")

            // assert
            assertThat(result).isFalse()
        }
    }

    @DisplayName("이벤트를 처리 완료로 기록할 때,")
    @Nested
    inner class MarkHandled {

        @DisplayName("EventHandled가 저장된다.")
        @Test
        fun savesEventHandled() {
            // arrange
            whenever(eventHandledRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            idempotencyService.markHandled("event-1", "PRODUCT", "1", "PRODUCT_LIKED")

            // assert
            verify(eventHandledRepository).save(any())
        }
    }
}
