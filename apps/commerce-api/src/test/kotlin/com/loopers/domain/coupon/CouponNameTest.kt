package com.loopers.domain.coupon

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CouponNameTest {

    @Test
    fun `정상적인 쿠폰명의 경우 생성이 성공해야 한다`() {
        val name = CouponName("신규 가입 축하 쿠폰")

        assertThat(name.value).isEqualTo("신규 가입 축하 쿠폰")
    }

    @Test
    fun `빈 문자열의 경우 생성이 실패해야 한다`() {
        assertThatThrownBy { CouponName("") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `공백 문자열의 경우 생성이 실패해야 한다`() {
        assertThatThrownBy { CouponName("   ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `100자 초과의 경우 생성이 실패해야 한다`() {
        val longName = "가".repeat(101)

        assertThatThrownBy { CouponName(longName) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `100자인 경우 생성이 성공해야 한다`() {
        val name = CouponName("가".repeat(100))

        assertThat(name.value.length).isEqualTo(100)
    }
}
