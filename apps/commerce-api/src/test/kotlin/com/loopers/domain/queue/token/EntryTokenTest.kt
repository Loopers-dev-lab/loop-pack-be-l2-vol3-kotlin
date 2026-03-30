package com.loopers.domain.queue.token

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.model.EntryToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EntryTokenTest {

    @Nested
    @DisplayName("EntryToken 발급")
    inner class Issue {

        @Test
        @DisplayName("userId로 토큰을 발급하면 UUID 기반 토큰이 생성된다")
        fun `userId로 토큰을 발급하면 UUID 기반 토큰이 생성된다`() {
            // Arrange
            val userId = UserId(1L)

            // Act
            val token = EntryToken.issue(userId)

            // Assert
            assertThat(token.userId).isEqualTo(UserId(1L))
            assertThat(token.token).isNotBlank()
            assertThat(token.token).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        }

        @Test
        @DisplayName("동일 userId로 발급해도 매번 다른 토큰이 생성된다")
        fun `동일 userId로 발급해도 매번 다른 토큰이 생성된다`() {
            // Arrange & Act
            val token1 = EntryToken.issue(UserId(1L))
            val token2 = EntryToken.issue(UserId(1L))

            // Assert
            assertThat(token1.token).isNotEqualTo(token2.token)
        }

        @Test
        @DisplayName("기본 TTL은 300초이다")
        fun `기본 TTL은 300초이다`() {
            // Act & Assert
            assertThat(EntryToken.defaultTtlSeconds()).isEqualTo(300L)
        }
    }
}
