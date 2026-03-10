package com.loopers.interfaces.api

import com.loopers.domain.catalog.BrandModel
import com.loopers.domain.catalog.ProductModel
import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.user.Email
import com.loopers.domain.user.Password
import com.loopers.domain.user.UserModel
import com.loopers.domain.user.Username
import com.loopers.infrastructure.catalog.BrandJpaRepository
import com.loopers.infrastructure.catalog.ProductJpaRepository
import com.loopers.infrastructure.order.OrderItemJpaRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.ZonedDateTime

@SpringBootTest
@AutoConfigureMockMvc
class OrderV1UserApiE2ETest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val orderJpaRepository: OrderJpaRepository,
    private val orderItemJpaRepository: OrderItemJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_BASE = "/api/v1/orders"
        private const val DEFAULT_USERNAME = "testuser"
        private const val DEFAULT_PASSWORD = "password1234"
        private const val DEFAULT_NAME = "테스트유저"
        private const val DEFAULT_EMAIL = "test@example.com"
        private val DEFAULT_BIRTH_DATE: ZonedDateTime = ZonedDateTime.parse("1990-01-01T00:00:00+09:00")
        private val DEFAULT_PRODUCT_PRICE = BigDecimal("129000")
        private const val DEFAULT_PRODUCT_NAME = "에어맥스 90"
        private const val DEFAULT_ORDER_ITEM_QUANTITY = 2
        private const val DEFAULT_PRODUCT_QUANTITY = 10
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createUser(username: String = DEFAULT_USERNAME): UserModel {
        val user = UserModel(
            username = Username.of(username),
            password = Password.of(DEFAULT_PASSWORD, DEFAULT_BIRTH_DATE),
            name = DEFAULT_NAME,
            email = Email.of(DEFAULT_EMAIL),
            birthDate = DEFAULT_BIRTH_DATE,
        )
        user.applyEncodedPassword(passwordEncoder.encode(DEFAULT_PASSWORD))
        return userJpaRepository.save(user)
    }

    private fun createBrand(): BrandModel {
        return brandJpaRepository.save(BrandModel(name = "나이키"))
    }

    private fun createProduct(brandId: Long, quantity: Int = DEFAULT_PRODUCT_QUANTITY): ProductModel {
        return productJpaRepository.save(
            ProductModel(
                brandId = brandId,
                name = DEFAULT_PRODUCT_NAME,
                quantity = quantity,
                price = DEFAULT_PRODUCT_PRICE,
            ),
        )
    }

    private fun createOrder(userId: Long, status: OrderStatus = OrderStatus.ORDERED): OrderModel {
        val originalPrice = DEFAULT_PRODUCT_PRICE * BigDecimal(DEFAULT_ORDER_ITEM_QUANTITY)
        return orderJpaRepository.save(
            OrderModel(userId = userId, status = status, originalPrice = originalPrice, totalPrice = originalPrice),
        )
    }

    private fun createOrderItem(orderId: Long, productId: Long): OrderItemModel {
        return orderItemJpaRepository.save(
            OrderItemModel(
                orderId = orderId,
                productId = productId,
                productName = DEFAULT_PRODUCT_NAME,
                quantity = DEFAULT_ORDER_ITEM_QUANTITY,
                price = DEFAULT_PRODUCT_PRICE,
            ),
        )
    }

    @DisplayName("PATCH /api/v1/orders/{orderId}/cancel")
    @Nested
    inner class CancelOrder {
        @DisplayName("ORDERED 상태 주문을 취소하면, 200 OK와 취소된 주문 ID를 반환한다.")
        @Test
        fun returnsOkAndCancelledOrderId() {
            // arrange
            val stockAfterOrder = DEFAULT_PRODUCT_QUANTITY - DEFAULT_ORDER_ITEM_QUANTITY

            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brandId = brand.id, quantity = stockAfterOrder)
            val order = createOrder(userId = user.id)
            createOrderItem(orderId = order.id, productId = product.id)

            // act
            mockMvc.perform(
                patch("$ENDPOINT_BASE/${order.id}/cancel")
                    .header("X-Loopers-LoginId", DEFAULT_USERNAME)
                    .header("X-Loopers-LoginPw", DEFAULT_PASSWORD),
            )
                // assert
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.id").value(order.id))

            val cancelledOrder = orderJpaRepository.findById(order.id).get()
            val restoredProduct = productJpaRepository.findById(product.id).get()

            assertAll(
                { assertThat(cancelledOrder.status).isEqualTo(OrderStatus.CANCELLED) },
                { assertThat(restoredProduct.quantity).isEqualTo(DEFAULT_PRODUCT_QUANTITY) },
            )
        }

        @DisplayName("다른 사용자의 주문을 취소하면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorizedWhenCancellingOtherUsersOrder() {
            // arrange
            val owner = createUser("owner")
            createUser("other")
            val brand = createBrand()
            val product = createProduct(brandId = brand.id)
            val order = createOrder(userId = owner.id)
            createOrderItem(orderId = order.id, productId = product.id)

            // act & assert
            mockMvc.perform(
                patch("$ENDPOINT_BASE/${order.id}/cancel")
                    .header("X-Loopers-LoginId", "other")
                    .header("X-Loopers-LoginPw", DEFAULT_PASSWORD),
            )
                .andExpect(status().isUnauthorized)
        }

        @DisplayName("존재하지 않는 주문을 취소하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFoundWhenOrderDoesNotExist() {
            // arrange
            createUser()

            // act & assert
            mockMvc.perform(
                patch("$ENDPOINT_BASE/999/cancel")
                    .header("X-Loopers-LoginId", DEFAULT_USERNAME)
                    .header("X-Loopers-LoginPw", DEFAULT_PASSWORD),
            )
                .andExpect(status().isNotFound)
        }

        @DisplayName("CANCELLED 상태 주문을 취소하면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequestWhenStatusIsCancelled() {
            // arrange
            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brandId = brand.id)
            val order = createOrder(userId = user.id, status = OrderStatus.CANCELLED)
            createOrderItem(orderId = order.id, productId = product.id)

            // act & assert
            mockMvc.perform(
                patch("$ENDPOINT_BASE/${order.id}/cancel")
                    .header("X-Loopers-LoginId", DEFAULT_USERNAME)
                    .header("X-Loopers-LoginPw", DEFAULT_PASSWORD),
            )
                .andExpect(status().isBadRequest)
        }
    }
}
