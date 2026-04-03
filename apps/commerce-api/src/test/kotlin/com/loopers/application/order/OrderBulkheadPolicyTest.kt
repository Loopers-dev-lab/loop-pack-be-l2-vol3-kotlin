package com.loopers.application.order

import io.github.resilience4j.bulkhead.BulkheadRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@DisplayName("주문 Bulkhead 정책")
class OrderBulkheadPolicyTest @Autowired constructor(
    private val bulkheadRegistry: BulkheadRegistry,
) {

    @DisplayName("주문 처리에 Bulkhead가 적용되어 동시 요청 수가 제한되어야 한다")
    @Test
    fun orderPlaceShouldHaveBulkhead() {
        // act
        val bulkheadOptional = bulkheadRegistry.find("order-place")

        // assert
        assertThat(bulkheadOptional).isPresent
    }
}
