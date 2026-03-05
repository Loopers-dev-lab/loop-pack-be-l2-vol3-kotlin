package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("IdempotencyKey VO")
class IdempotencyKeyTest {

    @Nested
    @DisplayName("생성")
    inner class Create {

        @Test
        @DisplayName("유효한 값으로 생성 성공")
        fun create_success() {
            // act
            val key = IdempotencyKey("order-123-abc")

            // assert
            assertThat(key.value).isEqualTo("order-123-abc")
        }

        @Test
        @DisplayName("64자 이하 값으로 생성 성공 (경계값)")
        fun create_maxLength() {
            // arrange
            val value = "a".repeat(64)

            // act
            val key = IdempotencyKey(value)

            // assert
            assertThat(key.value).hasSize(64)
        }
    }

    @Nested
    @DisplayName("blank 값이면 생성 실패한다")
    inner class WhenBlank {

        @Test
        @DisplayName("빈 문자열 → 실패")
        fun create_emptyString() {
            val exception = assertThrows<CoreException> {
                IdempotencyKey("")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.ORDER_INVALID_IDEMPOTENCY_KEY)
        }

        @Test
        @DisplayName("공백 문자열 → 실패")
        fun create_blankString() {
            val exception = assertThrows<CoreException> {
                IdempotencyKey("   ")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.ORDER_INVALID_IDEMPOTENCY_KEY)
        }
    }

    @Nested
    @DisplayName("64자 초과이면 생성 실패한다")
    inner class WhenExceedsMaxLength {

        @Test
        @DisplayName("65자 → 실패")
        fun create_exceedsMaxLength() {
            val exception = assertThrows<CoreException> {
                IdempotencyKey("a".repeat(65))
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.ORDER_INVALID_IDEMPOTENCY_KEY)
        }
    }
}
