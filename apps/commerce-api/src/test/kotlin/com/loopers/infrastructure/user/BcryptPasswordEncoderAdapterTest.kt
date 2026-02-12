package com.loopers.infrastructure.user

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * BCrypt 학습 테스트.
 * Spring Security BCryptPasswordEncoder 동작 방식 검증.
 */
class BcryptPasswordEncoderAdapterTest {

    private val encoder = BcryptPasswordEncoderAdapter()

    @Test
    fun `encode된 비밀번호는 matches로 검증할 수 있어야 한다`() {
        val raw = "Password1!"
        val encoded = encoder.encode(raw)

        assertThat(encoder.matches(raw, encoded)).isTrue()
    }

    @Test
    fun `다른 비밀번호는 matches에서 false를 반환해야 한다`() {
        val encoded = encoder.encode("Password1!")

        assertThat(encoder.matches("WrongPass!", encoded)).isFalse()
    }

    @Test
    fun `같은 평문이라도 encode할 때마다 다른 해시가 생성되어야 한다`() {
        val raw = "Password1!"

        val encoded1 = encoder.encode(raw)
        val encoded2 = encoder.encode(raw)

        // salt가 매번 다르므로 해시값도 다름
        assertThat(encoded1).isNotEqualTo(encoded2)

        // 하지만 둘 다 원본과 매칭됨
        assertThat(encoder.matches(raw, encoded1)).isTrue()
        assertThat(encoder.matches(raw, encoded2)).isTrue()
    }

    @Test
    fun `BCrypt 해시는 $2a$ 접두사로 시작해야 한다`() {
        val encoded = encoder.encode("Password1!")

        // BCrypt 해시 형식: $2a$10$...
        assertThat(encoded).startsWith("\$2a\$")
    }
}
