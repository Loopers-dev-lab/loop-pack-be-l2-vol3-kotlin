package com.loopers.domain.product

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class StockTest {

    @Test
    fun `0개의 경우 Stock 생성이 성공해야 한다`() {
        val stock = Stock(0)

        assertThat(stock.quantity).isEqualTo(0)
    }

    @Test
    fun `양수 수량의 경우 Stock 생성이 성공해야 한다`() {
        val stock = Stock(100)

        assertThat(stock.quantity).isEqualTo(100)
    }

    @Test
    fun `음수 수량의 경우 Stock 생성이 실패해야 한다`() {
        assertThatThrownBy { Stock(-1) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `충분한 재고의 경우 decrease가 차감된 Stock을 반환해야 한다`() {
        val stock = Stock(10)

        val result = stock.decrease(3)

        assertThat(result.quantity).isEqualTo(7)
    }

    @Test
    fun `재고와 동일한 수량의 경우 decrease가 성공해야 한다`() {
        val stock = Stock(10)

        val result = stock.decrease(10)

        assertThat(result.quantity).isEqualTo(0)
    }

    @Test
    fun `재고 부족의 경우 decrease가 실패해야 한다`() {
        val stock = Stock(3)

        assertThatThrownBy { stock.decrease(5) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `차감 수량이 0의 경우 decrease가 실패해야 한다`() {
        val stock = Stock(10)

        assertThatThrownBy { stock.decrease(0) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `increase 호출시 증가된 Stock을 반환해야 한다`() {
        val stock = Stock(10)

        val result = stock.increase(5)

        assertThat(result.quantity).isEqualTo(15)
    }

    @Test
    fun `증가 수량이 0의 경우 increase가 실패해야 한다`() {
        val stock = Stock(10)

        assertThatThrownBy { stock.increase(0) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `충분한 재고의 경우 isEnough가 true를 반환해야 한다`() {
        val stock = Stock(10)

        assertThat(stock.isEnough(5)).isTrue()
    }

    @Test
    fun `부족한 재고의 경우 isEnough가 false를 반환해야 한다`() {
        val stock = Stock(3)

        assertThat(stock.isEnough(5)).isFalse()
    }

    @Test
    fun `동일한 수량의 경우 isEnough가 true를 반환해야 한다`() {
        val stock = Stock(5)

        assertThat(stock.isEnough(5)).isTrue()
    }
}
