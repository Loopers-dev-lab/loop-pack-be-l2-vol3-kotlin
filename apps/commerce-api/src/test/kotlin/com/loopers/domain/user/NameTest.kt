package com.loopers.domain.user

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class NameTest {

    @Test
    fun `올바른 이름의 경우 Name 생성이 성공해야 한다`() {
        val name = Name("신형기")
        assertThat(name.value).isEqualTo("신형기")
    }

    @Test
    fun `빈 문자열의 경우 Name 생성이 실패해야 한다`() {
        assertThatThrownBy { Name("") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `공백만 있는 경우 Name 생성이 실패해야 한다`() {
        assertThatThrownBy { Name("   ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `masked 호출시 마지막 글자가 마스킹되어야 한다`() {
        val name = Name("신형기")
        assertThat(name.masked()).isEqualTo("신형*")
    }

    @Test
    fun `한 글자 이름의 경우 masked 호출시 별표만 반환해야 한다`() {
        val name = Name("신")
        assertThat(name.masked()).isEqualTo("*")
    }
}
