package com.loopers.domain.product

import com.loopers.domain.product.vo.StockQuantity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class StockQuantityTest {
    @DisplayName("재고 수량을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("0 이상이면, 정상적으로 생성된다.")
        @Test
        fun createsStockQuantity_whenValid() {
            val qty = assertDoesNotThrow { StockQuantity.of(100) }
            assertThat(qty.value).isEqualTo(100)
        }

        @DisplayName("0이면, 정상적으로 생성된다.")
        @Test
        fun createsStockQuantity_whenZero() {
            val qty = assertDoesNotThrow { StockQuantity.of(0) }
            assertThat(qty.value).isEqualTo(0)
        }
    }
}
