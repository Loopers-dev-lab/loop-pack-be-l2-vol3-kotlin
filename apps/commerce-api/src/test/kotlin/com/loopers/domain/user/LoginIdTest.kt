package com.loopers.domain.user

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class LoginIdTest {

    @Test
    fun `올바른 영문 숫자 조합의 경우 LoginId 생성이 성공해야 한다`() {
        val loginId = LoginId("hyungki99")
        assertThat(loginId.value).isEqualTo("hyungki99")
    }

    @Test
    fun `10자 이내의 경우 LoginId 생성이 성공해야 한다`() {
        val loginId = LoginId("abcde12345")
        assertThat(loginId.value).isEqualTo("abcde12345")
    }

    @Test
    fun `11자 초과의 경우 LoginId 생성이 실패해야 한다`() {
        assertThatThrownBy { LoginId("abcde123456") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `특수문자 포함의 경우 LoginId 생성이 실패해야 한다`() {
        assertThatThrownBy { LoginId("hyung_ki") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `빈 문자열의 경우 LoginId 생성이 실패해야 한다`() {
        assertThatThrownBy { LoginId("") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
