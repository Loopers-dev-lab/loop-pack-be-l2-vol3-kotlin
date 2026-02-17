package com.loopers.domain.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandName
import com.loopers.domain.product.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductName
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.product.Stock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class OrderItemTest {

    @Test
    fun `create로 생성한 OrderItem의 persistenceId는 null이어야 한다`() {
        val orderItem = OrderItem.create(
            product = createProduct(),
            brand = createBrand(),
            quantity = QUANTITY,
        )

        assertThat(orderItem.persistenceId).isNull()
    }

    @Test
    fun `create로 생성한 OrderItem은 상품과 브랜드 정보를 스냅샷으로 보존해야 한다`() {
        val orderItem = OrderItem.create(
            product = createProduct(),
            brand = createBrand(),
            quantity = QUANTITY,
        )

        assertThat(orderItem.productId).isEqualTo(PRODUCT_ID)
        assertThat(orderItem.productName).isEqualTo(PRODUCT_NAME)
        assertThat(orderItem.brandName).isEqualTo(BRAND_NAME)
        assertThat(orderItem.price.amount).isEqualTo(PRICE)
        assertThat(orderItem.quantity).isEqualTo(QUANTITY)
    }

    @Test
    fun `수량이 0의 경우 create가 실패해야 한다`() {
        assertThatThrownBy {
            OrderItem.create(
                product = createProduct(),
                brand = createBrand(),
                quantity = 0,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `음수 수량의 경우 create가 실패해야 한다`() {
        assertThatThrownBy {
            OrderItem.create(
                product = createProduct(),
                brand = createBrand(),
                quantity = -1,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `getSubtotal 호출시 가격과 수량의 곱을 반환해야 한다`() {
        val orderItem = OrderItem.create(
            product = createProduct(),
            brand = createBrand(),
            quantity = QUANTITY,
        )

        val subtotal = orderItem.getSubtotal()

        assertThat(subtotal.amount).isEqualTo(PRICE * QUANTITY)
    }

    @Test
    fun `reconstitute로 생성한 OrderItem은 persistenceId를 가져야 한다`() {
        val orderItem = OrderItem.reconstitute(
            persistenceId = 1L,
            productId = PRODUCT_ID,
            productName = PRODUCT_NAME,
            brandName = BRAND_NAME,
            price = Money(PRICE),
            quantity = QUANTITY,
        )

        assertThat(orderItem.persistenceId).isEqualTo(1L)
    }

    @Test
    fun `저장되지 않은 상품으로 create시 실패해야 한다`() {
        val unsavedProduct = Product.create(
            brandId = 1L,
            name = ProductName(PRODUCT_NAME),
            description = null,
            price = Money(PRICE),
            stock = Stock(100),
            thumbnailUrl = null,
            images = emptyList(),
        )

        assertThatThrownBy {
            OrderItem.create(
                product = unsavedProduct,
                brand = createBrand(),
                quantity = QUANTITY,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun createProduct(): Product = Product.reconstitute(
        persistenceId = PRODUCT_ID,
        brandId = 1L,
        name = ProductName(PRODUCT_NAME),
        description = null,
        price = Money(PRICE),
        stock = Stock(100),
        thumbnailUrl = null,
        status = ProductStatus.ACTIVE,
        likeCount = 0,
        deletedAt = null,
        images = emptyList(),
    )

    private fun createBrand(): Brand = Brand.reconstitute(
        persistenceId = 1L,
        name = BrandName(BRAND_NAME),
        description = null,
        logoUrl = null,
        status = com.loopers.domain.brand.BrandStatus.ACTIVE,
        deletedAt = null,
    )

    companion object {
        private const val PRODUCT_ID = 10L
        private const val PRODUCT_NAME = "에어맥스 90"
        private const val BRAND_NAME = "나이키"
        private const val PRICE = 129000L
        private const val QUANTITY = 2
    }
}
