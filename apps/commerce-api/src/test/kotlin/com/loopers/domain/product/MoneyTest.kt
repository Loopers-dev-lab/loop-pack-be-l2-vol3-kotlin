package com.loopers.domain.product

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class MoneyTest {

    @Test
    fun `0원의 경우 Money 생성이 성공해야 한다`() {
        val money = Money(0)

        assertThat(money.amount).isEqualTo(0)
    }

    @Test
    fun `양수 금액의 경우 Money 생성이 성공해야 한다`() {
        val money = Money(10000)

        assertThat(money.amount).isEqualTo(10000)
    }

    @Test
    fun `음수 금액의 경우 Money 생성이 실패해야 한다`() {
        assertThatThrownBy { Money(-1) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `multiply 호출시 수량만큼 곱한 금액을 반환해야 한다`() {
        val money = Money(1000)

        val result = money.multiply(3)

        assertThat(result.amount).isEqualTo(3000)
    }

    @Test
    fun `add 호출시 두 금액의 합을 반환해야 한다`() {
        val money1 = Money(1000)
        val money2 = Money(2000)

        val result = money1.add(money2)

        assertThat(result.amount).isEqualTo(3000)
    }

    @Test
    fun `multiply 호출시 새 Money 인스턴스를 반환해야 한다`() {
        val original = Money(1000)

        val result = original.multiply(2)

        assertThat(result).isNotSameAs(original)
        assertThat(original.amount).isEqualTo(1000)
    }

    @Test
    fun `add 호출시 새 Money 인스턴스를 반환해야 한다`() {
        val original = Money(1000)

        val result = original.add(Money(500))

        assertThat(result).isNotSameAs(original)
        assertThat(original.amount).isEqualTo(1000)
    }
}
