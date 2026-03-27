package com.loopers.infrastructure.payment

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PgCallbackSignatureVerifier")
class PgCallbackSignatureVerifierTest {
    private val verifier = PgCallbackSignatureVerifier("callback-secret")

    @DisplayName("verify")
    @Nested
    inner class Verify {
        @DisplayName("서명이 유효하면 true를 반환한다")
        @Test
        fun returnsTrue_whenSignatureIsValid() {
            val payload = "100|APPROVED|pg-100|"
            val signature = verifier.sign(payload)

            val result = verifier.verify(payload, signature)

            assertThat(result).isTrue()
        }

        @DisplayName("서명이 유효하지 않으면 false를 반환한다")
        @Test
        fun returnsFalse_whenSignatureIsInvalid() {
            val result = verifier.verify("100|APPROVED|pg-100|", "invalid-signature")

            assertThat(result).isFalse()
        }
    }
}
