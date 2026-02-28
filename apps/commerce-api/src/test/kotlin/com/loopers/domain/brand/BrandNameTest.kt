package com.loopers.domain.brand

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class BrandNameTest {

    @Test
    fun `올바른 이름의 경우 BrandName 생성이 성공해야 한다`() {
        val brandName = BrandName("나이키")

        assertThat(brandName.value).isEqualTo("나이키")
    }

    @Test
    fun `빈 문자열의 경우 BrandName 생성이 실패해야 한다`() {
        assertThatThrownBy { BrandName("") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `공백만 있는 경우 BrandName 생성이 실패해야 한다`() {
        assertThatThrownBy { BrandName("   ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `100자의 경우 BrandName 생성이 성공해야 한다`() {
        val validName = "가".repeat(100)
        val brandName = BrandName(validName)

        assertThat(brandName.value).isEqualTo(validName)
    }

    @Test
    fun `101자의 경우 BrandName 생성이 실패해야 한다`() {
        val overLengthName = "가".repeat(101)

        assertThatThrownBy { BrandName(overLengthName) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
