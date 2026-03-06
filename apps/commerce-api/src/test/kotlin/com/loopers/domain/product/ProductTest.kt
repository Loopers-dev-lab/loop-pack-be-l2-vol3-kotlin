package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ProductErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class ProductTest {

    private fun createProduct(
        brandId: Long = 1L,
        name: String = "테스트 상품",
        description: String = "상품 설명",
        price: Money = Money(10000),
        imageUrl: String = "https://example.com/image.jpg",
    ): Product {
        return Product.create(
            brandId = brandId,
            name = name,
            description = description,
            price = price,
            imageUrl = imageUrl,
        )
    }

    @DisplayName("상품 생성")
    @Nested
    inner class Create {

        @DisplayName("정상 입력이면 상품이 생성된다")
        @Test
        fun success() {
            val product = createProduct()

            assertAll(
                { assertThat(product.brandId).isEqualTo(1L) },
                { assertThat(product.name).isEqualTo("테스트 상품") },
                { assertThat(product.description).isEqualTo("상품 설명") },
                { assertThat(product.price).isEqualTo(Money(10000)) },
                { assertThat(product.likeCount).isEqualTo(0) },
                { assertThat(product.isDeleted()).isFalse() },
            )
        }

        @DisplayName("이름이 빈값이면 INVALID_PRODUCT_NAME 예외가 발생한다")
        @Test
        fun failWhenNameEmpty() {
            val exception = assertThrows<CoreException> {
                createProduct(name = "")
            }

            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.INVALID_PRODUCT_NAME)
        }

        @DisplayName("이름이 공백으로만 이루어져 있으면 INVALID_PRODUCT_NAME 예외가 발생한다")
        @Test
        fun failWhenNameBlank() {
            val exception = assertThrows<CoreException> {
                createProduct(name = "   ")
            }

            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.INVALID_PRODUCT_NAME)
        }

        @DisplayName("이름이 100자를 초과하면 INVALID_PRODUCT_NAME 예외가 발생한다")
        @Test
        fun failWhenNameTooLong() {
            val exception = assertThrows<CoreException> {
                createProduct(name = "가".repeat(101))
            }

            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.INVALID_PRODUCT_NAME)
        }
    }

    @DisplayName("상품 수정")
    @Nested
    inner class Update {

        @DisplayName("정상 입력이면 수정에 성공한다")
        @Test
        fun success() {
            val product = createProduct()

            product.update(
                name = "수정된 상품",
                description = "수정된 설명",
                price = Money(20000),
                imageUrl = "https://example.com/new.jpg",
            )

            assertAll(
                { assertThat(product.name).isEqualTo("수정된 상품") },
                { assertThat(product.description).isEqualTo("수정된 설명") },
                { assertThat(product.price).isEqualTo(Money(20000)) },
                { assertThat(product.imageUrl).isEqualTo("https://example.com/new.jpg") },
            )
        }

        @DisplayName("수정 시 brandId는 변경되지 않는다")
        @Test
        fun brandIdNotChanged() {
            val product = createProduct(brandId = 1L)

            product.update(
                name = "수정된 상품",
                description = "수정된 설명",
                price = Money(20000),
                imageUrl = "https://example.com/new.jpg",
            )

            assertThat(product.brandId).isEqualTo(1L)
        }

        @DisplayName("수정 시 이름이 빈값이면 INVALID_PRODUCT_NAME 예외가 발생한다")
        @Test
        fun failWhenNameEmpty() {
            val product = createProduct()

            val exception = assertThrows<CoreException> {
                product.update(
                    name = "",
                    description = "설명",
                    price = Money(10000),
                    imageUrl = "https://example.com/image.jpg",
                )
            }

            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.INVALID_PRODUCT_NAME)
        }

        @DisplayName("수정 시 이름이 100자를 초과하면 INVALID_PRODUCT_NAME 예외가 발생한다")
        @Test
        fun failWhenNameTooLong() {
            val product = createProduct()

            val exception = assertThrows<CoreException> {
                product.update(
                    name = "가".repeat(101),
                    description = "설명",
                    price = Money(10000),
                    imageUrl = "https://example.com/image.jpg",
                )
            }

            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.INVALID_PRODUCT_NAME)
        }
    }

    @DisplayName("상품 삭제")
    @Nested
    inner class Delete {

        @DisplayName("삭제하면 isDeleted()가 true를 반환한다")
        @Test
        fun deleteSuccess() {
            val product = createProduct()

            product.delete()

            assertThat(product.isDeleted()).isTrue()
        }
    }

}
