package com.loopers.domain.product

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ProductTest {

    @Test
    fun `create로 생성한 Product의 persistenceId는 null이어야 한다`() {
        val product = createProduct()

        assertThat(product.persistenceId).isNull()
    }

    @Test
    fun `create로 생성한 Product의 상태는 ACTIVE여야 한다`() {
        val product = createProduct()

        assertThat(product.status).isEqualTo(ProductStatus.ACTIVE)
    }

    @Test
    fun `create로 생성한 Product의 likeCount는 0이어야 한다`() {
        val product = createProduct()

        assertThat(product.likeCount).isEqualTo(0)
    }

    @Test
    fun `create로 생성한 Product의 deletedAt은 null이어야 한다`() {
        val product = createProduct()

        assertThat(product.deletedAt).isNull()
    }

    @Test
    fun `reconstitute로 생성한 Product는 persistenceId를 가져야 한다`() {
        val product = Product.reconstitute(
            persistenceId = 1L,
            brandId = BRAND_ID,
            name = ProductName(PRODUCT_NAME),
            description = DESCRIPTION,
            price = Money(PRICE),
            stock = Stock(STOCK_QUANTITY),
            thumbnailUrl = THUMBNAIL_URL,
            status = ProductStatus.ACTIVE,
            likeCount = 10,
            deletedAt = null,
            images = emptyList(),
        )

        assertThat(product.persistenceId).isEqualTo(1L)
        assertThat(product.likeCount).isEqualTo(10)
    }

    @Test
    fun `likeCount가 음수인 경우 생성이 실패해야 한다`() {
        assertThatThrownBy {
            Product.reconstitute(
                persistenceId = 1L,
                brandId = BRAND_ID,
                name = ProductName(PRODUCT_NAME),
                description = DESCRIPTION,
                price = Money(PRICE),
                stock = Stock(STOCK_QUANTITY),
                thumbnailUrl = THUMBNAIL_URL,
                status = ProductStatus.ACTIVE,
                likeCount = -1,
                deletedAt = null,
                images = emptyList(),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `정상적인 경우 update가 새 Product를 반환해야 한다`() {
        val product = createProduct()

        val updated = product.update(
            name = ProductName(UPDATED_NAME),
            description = "수정된 설명",
            price = Money(199000),
            stock = Stock(50),
            thumbnailUrl = "https://example.com/new-thumb.png",
            status = ProductStatus.INACTIVE,
            images = emptyList(),
        )

        assertThat(updated).isNotSameAs(product)
        assertThat(updated.name.value).isEqualTo(UPDATED_NAME)
        assertThat(updated.price.amount).isEqualTo(199000)
    }

    @Test
    fun `update시 brandId는 변경되지 않아야 한다`() {
        val product = createProduct()

        val updated = product.update(
            name = ProductName(UPDATED_NAME),
            description = "수정된 설명",
            price = Money(199000),
            stock = Stock(50),
            thumbnailUrl = null,
            status = ProductStatus.ACTIVE,
            images = emptyList(),
        )

        assertThat(updated.brandId).isEqualTo(BRAND_ID)
    }

    @Test
    fun `삭제된 상품의 경우 update가 실패해야 한다`() {
        val product = createProduct().delete()

        assertThatThrownBy {
            product.update(
                name = ProductName(UPDATED_NAME),
                description = "수정된 설명",
                price = Money(199000),
                stock = Stock(50),
                thumbnailUrl = null,
                status = ProductStatus.ACTIVE,
                images = emptyList(),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `delete 호출시 deletedAt이 설정되어야 한다`() {
        val product = createProduct()

        val deleted = product.delete()

        assertThat(deleted.deletedAt).isNotNull()
    }

    @Test
    fun `delete 호출시 isDeleted가 true를 반환해야 한다`() {
        val product = createProduct()

        val deleted = product.delete()

        assertThat(deleted.isDeleted()).isTrue()
    }

    @Test
    fun `삭제되지 않고 재고 충분한 경우 canOrder가 true를 반환해야 한다`() {
        val product = createProduct()

        assertThat(product.canOrder(10)).isTrue()
    }

    @Test
    fun `재고 부족의 경우 canOrder가 false를 반환해야 한다`() {
        val product = createProduct()

        assertThat(product.canOrder(STOCK_QUANTITY + 1)).isFalse()
    }

    @Test
    fun `삭제된 상품의 경우 canOrder가 false를 반환해야 한다`() {
        val product = createProduct().delete()

        assertThat(product.canOrder(1)).isFalse()
    }

    @Test
    fun `재고 충분하지만 삭제된 경우 canOrder가 false를 반환해야 한다`() {
        val product = createProduct().delete()

        assertThat(product.canOrder(10)).isFalse()
    }

    @Test
    fun `deletedAt이 null인 경우 isDeleted가 false를 반환해야 한다`() {
        val product = createProduct()

        assertThat(product.isDeleted()).isFalse()
    }

    private fun createProduct(): Product = Product.create(
        brandId = BRAND_ID,
        name = ProductName(PRODUCT_NAME),
        description = DESCRIPTION,
        price = Money(PRICE),
        stock = Stock(STOCK_QUANTITY),
        thumbnailUrl = THUMBNAIL_URL,
        images = emptyList(),
    )

    companion object {
        private const val BRAND_ID = 1L
        private const val PRODUCT_NAME = "에어맥스 90"
        private const val DESCRIPTION = "클래식 러닝화"
        private const val THUMBNAIL_URL = "https://example.com/thumb.png"
        private const val PRICE = 129000L
        private const val STOCK_QUANTITY = 100
        private const val UPDATED_NAME = "에어포스 1"
    }
}
