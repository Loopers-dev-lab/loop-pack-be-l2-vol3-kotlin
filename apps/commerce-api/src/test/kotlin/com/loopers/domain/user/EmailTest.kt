package com.loopers.domain.user

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class EmailTest {

    @Test
    fun `올바른 이메일 형식의 경우 Email 생성이 성공해야 한다`() {
        val email = Email("hyungki@loopers.com")
        assertThat(email.value).isEqualTo("hyungki@loopers.com")
    }

    @Test
    fun `@ 없는 경우 Email 생성이 실패해야 한다`() {
        assertThatThrownBy { Email("hyungkiloopers.com") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `도메인 없는 경우 Email 생성이 실패해야 한다`() {
        assertThatThrownBy { Email("hyungki@") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `TLD 없는 경우 Email 생성이 실패해야 한다`() {
        assertThatThrownBy { Email("hyungki@loopers") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `빈 문자열의 경우 Email 생성이 실패해야 한다`() {
        assertThatThrownBy { Email("") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
