package com.loopers.domain.user

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BirthDateTest {

    @Test
    fun `올바른 yyyy-MM-dd 형식의 경우 BirthDate 생성이 성공해야 한다`() {
        val birthDate = BirthDate.from("1993-04-01")
        assertThat(birthDate.value).isEqualTo(LocalDate.of(1993, 4, 1))
    }

    @Test
    fun `구분자 없는 경우 BirthDate 생성이 실패해야 한다`() {
        assertThatThrownBy { BirthDate.from("19930401") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `슬래시 구분자의 경우 BirthDate 생성이 실패해야 한다`() {
        assertThatThrownBy { BirthDate.from("1993/04/01") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `존재하지 않는 날짜의 경우 BirthDate 생성이 실패해야 한다`() {
        assertThatThrownBy { BirthDate.from("2024-02-30") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `미래 날짜의 경우 BirthDate 생성이 실패해야 한다`() {
        val future = LocalDate.now().plusDays(1)
        assertThatThrownBy { BirthDate(future) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `toCompactString 호출시 YYYYMMDD 형식을 반환해야 한다`() {
        val birthDate = BirthDate.from("1993-04-01")
        assertThat(birthDate.toCompactString()).isEqualTo("19930401")
    }
}
