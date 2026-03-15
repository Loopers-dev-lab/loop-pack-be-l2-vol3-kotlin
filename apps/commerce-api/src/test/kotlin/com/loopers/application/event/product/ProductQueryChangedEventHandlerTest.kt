package com.loopers.application.event.product

import com.loopers.domain.product.ProductQueryInvalidator
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.then
import org.mockito.kotlin.mock

@DisplayName("ProductQueryChangedEventHandler")
class ProductQueryChangedEventHandlerTest {
    private val productQueryInvalidator: ProductQueryInvalidator = mock()
    private val handler = ProductQueryChangedEventHandler(productQueryInvalidator)

    @Test
    @DisplayName("productIds는 detail invalidation, brandIds는 목록 invalidation으로 해석한다")
    fun handle_event() {
        handler.handle(
            ProductQueryChangedEvent(
                productIds = listOf(1L, 2L, 2L),
                brandIds = listOf(10L, 10L, 20L),
            ),
        )

        then(productQueryInvalidator).should().invalidateDetails(listOf(1L, 2L))
        then(productQueryInvalidator).should().invalidateListsByBrandId(10L)
        then(productQueryInvalidator).should().invalidateListsByBrandId(20L)
    }
}
