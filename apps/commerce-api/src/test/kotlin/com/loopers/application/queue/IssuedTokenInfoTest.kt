package com.loopers.application.queue

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class IssuedTokenInfoTest {

    @Nested
    @DisplayName("toString")
    inner class ToString {

        @Test
        @DisplayName("toString에 원문 토큰이 포함되지 않는다")
        fun `toString에 원문 토큰이 포함되지 않는다`() {
            // Arrange
            val tokenInfo = IssuedTokenInfo(userId = 1L, token = "secret-token-value")

            // Act
            val result = tokenInfo.toString()

            // Assert
            assertThat(result).doesNotContain("secret-token-value")
        }
    }
}
