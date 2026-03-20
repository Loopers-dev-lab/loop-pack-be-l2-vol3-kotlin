package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("PaymentIdempotencyKey")
class PaymentIdempotencyKeyTest {

    @Nested
    @DisplayName("유효한 값이면 생성 성공한다")
    inner class WhenValid {

        @Test
        @DisplayName("정상 값 → 생성 성공")
        fun create_normalValue() {
            val key = PaymentIdempotencyKey("valid-key-123")
            assertThat(key.value).isEqualTo("valid-key-123")
        }

        @Test
        @DisplayName("64자 → 생성 성공 (경계값)")
        fun create_maxLength() {
            val key = PaymentIdempotencyKey("a".repeat(64))
            assertThat(key.value).hasSize(64)
        }
    }

    @Nested
    @DisplayName("유효하지 않은 값이면 생성 실패한다")
    inner class WhenInvalid {

        @Test
        @DisplayName("빈 문자열 → 예외")
        fun create_emptyString() {
            val exception = assertThrows<CoreException> {
                PaymentIdempotencyKey("")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_INVALID_IDEMPOTENCY_KEY)
        }

        @Test
        @DisplayName("공백만 있는 문자열 → 예외")
        fun create_blankString() {
            val exception = assertThrows<CoreException> {
                PaymentIdempotencyKey("   ")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_INVALID_IDEMPOTENCY_KEY)
        }

        @Test
        @DisplayName("65자 초과 → 예외")
        fun create_tooLong() {
            val exception = assertThrows<CoreException> {
                PaymentIdempotencyKey("a".repeat(65))
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_INVALID_IDEMPOTENCY_KEY)
        }
    }
}
