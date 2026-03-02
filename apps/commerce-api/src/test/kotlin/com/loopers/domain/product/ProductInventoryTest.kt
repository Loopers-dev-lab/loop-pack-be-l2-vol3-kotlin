package com.loopers.domain.product

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ProductInventoryTest {

    @Nested
    inner class Create {

        @Test
        fun `유효한 파라미터 입력 시 생성한다`() {
            val productId = 0L
            val stock = Stock(10_000)

            assertDoesNotThrow {
                ProductInventoryModel(productId, stock)
            }
        }
    }

    @Nested
    inner class Stock {

        @Test
        fun `재고 증가 시 기존 재고에 입력한 수량 만큼 추가된다`() {
            val inventory = ProductInventoryModel(
                productId = 0L,
                stock = Stock(0),
            )

            val beforeStock = inventory.stock.value
            val quantity = 10_000L
            inventory.increaseStock(quantity)

            assertThat(inventory.stock.value).isEqualTo(beforeStock + quantity)
        }

        @Test
        fun `재고 감소 시 기존 재고에 입력한 수량 만큼 감소한다`() {
            val inventory = ProductInventoryModel(
                productId = 0L,
                stock = Stock(10_000L),
            )

            val beforeStock = inventory.stock.value
            val quantity = 199L
            inventory.decreaseStock(quantity)

            assertThat(inventory.stock.value).isEqualTo(beforeStock - quantity)
        }

        @Test
        fun `재고 감소 시 재고가 부족하면 에러 반환한다`() {
            val inventory = ProductInventoryModel(
                productId = 0L,
                stock = Stock(1_000L),
            )

            val quantity = 1_001L
            assertThrows<IllegalArgumentException> { inventory.decreaseStock(quantity) }
        }
    }
}
