package com.loopers.event.payload

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OrderItemPayload")
class OrderItemPayloadTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()

    @DisplayName("JSON 직렬화/역직렬화 시,")
    @Nested
    inner class JsonSerialization {

        @DisplayName("unitPrice 필드가 포함되어 보존된다.")
        @Test
        fun preservesUnitPrice() {
            // arrange
            val payload = OrderItemPayload(
                productId = 1L,
                quantity = 3,
                productName = "테스트 상품",
                unitPrice = 15000L,
            )

            // act
            val json = objectMapper.writeValueAsString(payload)
            val deserialized = objectMapper.readValue(json, OrderItemPayload::class.java)

            // assert
            assertThat(deserialized.unitPrice).isEqualTo(15000L)
            assertThat(deserialized).isEqualTo(payload)
        }
    }
}
