package com.loopers.domain.product

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ProductNameTest {

    @Test
    fun `올바른 상품명의 경우 ProductName 생성이 성공해야 한다`() {
        val productName = ProductName("에어맥스 90")

        assertThat(productName.value).isEqualTo("에어맥스 90")
    }

    @Test
    fun `빈 문자열의 경우 ProductName 생성이 실패해야 한다`() {
        assertThatThrownBy { ProductName("") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `공백만 있는 경우 ProductName 생성이 실패해야 한다`() {
        assertThatThrownBy { ProductName("   ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `200자의 경우 ProductName 생성이 성공해야 한다`() {
        val validName = "가".repeat(200)
        val productName = ProductName(validName)

        assertThat(productName.value).isEqualTo(validName)
    }

    @Test
    fun `201자의 경우 ProductName 생성이 실패해야 한다`() {
        val overLengthName = "가".repeat(201)

        assertThatThrownBy { ProductName(overLengthName) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
