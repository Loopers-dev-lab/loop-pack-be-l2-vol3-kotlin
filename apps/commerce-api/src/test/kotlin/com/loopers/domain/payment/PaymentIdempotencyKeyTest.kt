package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

@DisplayName("PaymentIdempotencyKey")
class PaymentIdempotencyKeyTest {

    @Nested
    @DisplayName("유효한 값이면 생성 성공한다")
    inner class WhenValid {

        @Test
        @DisplayName("정상 값 → 생성 성공")
        fun create_normalValue() {
            val value = UUID.randomUUID().toString()
            val key = PaymentIdempotencyKey(value)
            assertThat(key.value).isEqualTo(value)
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
        @DisplayName("UUID 형식이 아니면 예외")
        fun create_invalidFormat() {
            val exception = assertThrows<CoreException> {
                PaymentIdempotencyKey("not-a-uuid")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.PAYMENT_INVALID_IDEMPOTENCY_KEY)
        }
    }
}
