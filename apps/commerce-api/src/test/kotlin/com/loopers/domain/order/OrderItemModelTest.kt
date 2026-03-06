package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OrderItemModel")
class OrderItemModelTest {

    companion object {
        private const val VALID_PRODUCT_ID = 1L
        private const val VALID_PRODUCT_NAME = "감성 티셔츠"
        private const val VALID_BRAND_NAME = "루프팩"
        private const val VALID_PRICE = 25000L
        private const val VALID_QUANTITY = 2
    }

    private fun createOrder(): OrderModel = OrderModel(userId = 1L)

    @DisplayName("정상 생성")
    @Nested
    inner class Create {
        @DisplayName("모든 필드가 유효하면 OrderItemModel이 생성되고, subTotal이 정확히 계산된다")
        @Test
        fun createsOrderItemModel_whenAllFieldsAreValid() {
            // arrange
            val order = createOrder()

            // act
            val item = OrderItemModel(
                order = order,
                productId = VALID_PRODUCT_ID,
                productName = VALID_PRODUCT_NAME,
                brandName = VALID_BRAND_NAME,
                price = VALID_PRICE,
                quantity = VALID_QUANTITY,
            )

            // assert
            assertThat(item.order).isEqualTo(order)
            assertThat(item.productId).isEqualTo(VALID_PRODUCT_ID)
            assertThat(item.productName).isEqualTo(VALID_PRODUCT_NAME)
            assertThat(item.brandName).isEqualTo(VALID_BRAND_NAME)
            assertThat(item.price).isEqualTo(VALID_PRICE)
            assertThat(item.quantity).isEqualTo(VALID_QUANTITY)
            assertThat(item.subTotal).isEqualTo(VALID_PRICE * VALID_QUANTITY)
        }

        @DisplayName("수량이 1일 때 subTotal은 가격과 동일하다")
        @Test
        fun subTotalEqualsPrice_whenQuantityIsOne() {
            // arrange & act
            val item = OrderItemModel(
                order = createOrder(),
                productId = VALID_PRODUCT_ID,
                productName = VALID_PRODUCT_NAME,
                brandName = VALID_BRAND_NAME,
                price = VALID_PRICE,
                quantity = 1,
            )

            // assert
            assertThat(item.subTotal).isEqualTo(VALID_PRICE)
        }

        @DisplayName("스냅샷 필드에 주문 시점의 상품 정보가 저장된다")
        @Test
        fun snapshotFieldsPreserveProductInfo() {
            // arrange
            val productName = "한정판 후드티"
            val brandName = "프리미엄브랜드"
            val price = 89000L

            // act
            val item = OrderItemModel(
                order = createOrder(),
                productId = VALID_PRODUCT_ID,
                productName = productName,
                brandName = brandName,
                price = price,
                quantity = 3,
            )

            // assert
            assertThat(item.productName).isEqualTo(productName)
            assertThat(item.brandName).isEqualTo(brandName)
            assertThat(item.price).isEqualTo(price)
            assertThat(item.subTotal).isEqualTo(89000L * 3)
        }
    }

    @DisplayName("수량 검증")
    @Nested
    inner class QuantityValidation {
        @DisplayName("수량이 0이면 예외가 발생한다")
        @Test
        fun throwsException_whenQuantityIsZero() {
            // arrange & act & assert
            assertThatThrownBy {
                OrderItemModel(
                    order = createOrder(),
                    productId = VALID_PRODUCT_ID,
                    productName = VALID_PRODUCT_NAME,
                    brandName = VALID_BRAND_NAME,
                    price = VALID_PRICE,
                    quantity = 0,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("수량이 음수이면 예외가 발생한다")
        @Test
        fun throwsException_whenQuantityIsNegative() {
            // arrange & act & assert
            assertThatThrownBy {
                OrderItemModel(
                    order = createOrder(),
                    productId = VALID_PRODUCT_ID,
                    productName = VALID_PRODUCT_NAME,
                    brandName = VALID_BRAND_NAME,
                    price = VALID_PRICE,
                    quantity = -1,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("수량이 100이면 예외가 발생한다")
        @Test
        fun throwsException_whenQuantityIs100() {
            // arrange & act & assert
            assertThatThrownBy {
                OrderItemModel(
                    order = createOrder(),
                    productId = VALID_PRODUCT_ID,
                    productName = VALID_PRODUCT_NAME,
                    brandName = VALID_BRAND_NAME,
                    price = VALID_PRICE,
                    quantity = 100,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("수량이 정확히 1이면 정상 생성된다")
        @Test
        fun createsOrderItemModel_whenQuantityIsOne() {
            // arrange & act
            val item = OrderItemModel(
                order = createOrder(),
                productId = VALID_PRODUCT_ID,
                productName = VALID_PRODUCT_NAME,
                brandName = VALID_BRAND_NAME,
                price = VALID_PRICE,
                quantity = 1,
            )

            // assert
            assertThat(item.quantity).isEqualTo(1)
        }

        @DisplayName("수량이 정확히 99이면 정상 생성된다")
        @Test
        fun createsOrderItemModel_whenQuantityIs99() {
            // arrange & act
            val item = OrderItemModel(
                order = createOrder(),
                productId = VALID_PRODUCT_ID,
                productName = VALID_PRODUCT_NAME,
                brandName = VALID_BRAND_NAME,
                price = VALID_PRICE,
                quantity = 99,
            )

            // assert
            assertThat(item.quantity).isEqualTo(99)
        }
    }
}
