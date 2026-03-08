package com.loopers.domain.stock

import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class StockTest {

    @Test
    @DisplayName("Stock 생성 성공")
    fun testCreateStock() {
        val stock = Stock.create(productId = 1L, quantity = 100)
        assertThat(stock.productId).isEqualTo(1L)
        assertThat(stock.quantity).isEqualTo(100)
    }

    @Test
    @DisplayName("음수 재고로는 Stock을 생성할 수 없다")
    fun testCreateStockWithNegativeQuantity() {
        assertThatThrownBy {
            Stock.create(productId = 1L, quantity = -1)
        }.isInstanceOf(CoreException::class.java)
    }

    @Test
    @DisplayName("재고를 감소시킨다")
    fun testMinusStock() {
        val stock = Stock.create(productId = 1L, quantity = 100)
        stock.minusStock(30)
        assertThat(stock.quantity).isEqualTo(70)
    }

    @Test
    @DisplayName("재고 부족 시 예외를 발생시킨다")
    fun testMinusStockWhenInsufficient() {
        val stock = Stock.create(productId = 1L, quantity = 10)
        assertThatThrownBy {
            stock.minusStock(20)
        }.isInstanceOf(CoreException::class.java)
    }

    @Test
    @DisplayName("재고를 증가시킨다")
    fun testPlusStock() {
        val stock = Stock.create(productId = 1L, quantity = 100)
        stock.plusStock(50)
        assertThat(stock.quantity).isEqualTo(150)
    }

    @Test
    @DisplayName("0개로는 재고를 감소시킬 수 없다")
    fun testMinusStockZeroQuantity() {
        val stock = Stock.create(productId = 1L, quantity = 100)
        assertThatThrownBy {
            stock.minusStock(0)
        }.isInstanceOf(CoreException::class.java)
    }
}
