package com.loopers.application.order

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.application.user.UserCommand
import com.loopers.domain.product.ProductRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.OrderErrorCode
import com.loopers.support.error.OrderValidationException
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class CreateOrderUseCaseTest @Autowired constructor(
    private val createOrderUseCase: CreateOrderUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val registerProductUseCase: RegisterProductUseCase,
    private val productRepository: ProductRepository,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerUser(loginId: String = "testuser"): Long {
        registerUserUseCase.execute(
            UserCommand.Register(
                loginId = loginId,
                rawPassword = "Test123!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            ),
        )
        return userJpaRepository.findByLoginId(loginId)!!.id
    }

    private fun registerBrand(name: String = "나이키"): Long {
        return registerBrandUseCase.execute(BrandCommand.Register(name = name)).id
    }

    private fun registerProduct(brandId: Long, name: String = "테스트 상품", price: Long = 10000, stock: Int = 100): Long {
        return registerProductUseCase.execute(
            ProductCommand.Register(
                brandId = brandId,
                name = name,
                description = "상품 설명",
                price = price,
                stock = stock,
                imageUrl = "https://example.com/image.jpg",
            ),
        ).id
    }

    @DisplayName("주문 생성")
    @Nested
    inner class Execute {

        @DisplayName("정상 주문 시 성공하고 재고가 차감된다")
        @Test
        fun success() {
            // arrange
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId, price = 10000, stock = 50)

            // act
            val result = createOrderUseCase.execute(
                OrderCommand.Create(
                    userId = userId,
                    items = listOf(
                        OrderCommand.Create.OrderLineItem(productId = productId, quantity = 3),
                    ),
                ),
            )

            // assert
            assertAll(
                { assertThat(result.totalAmount).isEqualTo(30000) },
                { assertThat(result.status).isEqualTo("ORDERED") },
            )

            val product = productRepository.findByIdOrNull(productId)
            assertThat(product?.stock?.quantity).isEqualTo(47)
        }

        @DisplayName("미존재 상품 포함 시 검증 실패")
        @Test
        fun failWhenProductNotFound() {
            val userId = registerUser()

            val exception = assertThrows<OrderValidationException> {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = listOf(
                            OrderCommand.Create.OrderLineItem(productId = 999L, quantity = 1),
                        ),
                    ),
                )
            }

            assertThat(exception.errors[0].reason).isEqualTo("PRODUCT_NOT_FOUND")
        }

        @DisplayName("삭제된 상품 포함 시 검증 실패")
        @Test
        fun failWhenProductDeleted() {
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId)

            val product = productRepository.findByIdOrNull(productId)!!
            product.delete()
            productRepository.save(product)

            val exception = assertThrows<OrderValidationException> {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = listOf(
                            OrderCommand.Create.OrderLineItem(productId = productId, quantity = 1),
                        ),
                    ),
                )
            }

            assertThat(exception.errors[0].reason).isEqualTo("PRODUCT_DELETED")
        }

        @DisplayName("재고 부족 시 검증 실패")
        @Test
        fun failWhenInsufficientStock() {
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId, stock = 5)

            val exception = assertThrows<OrderValidationException> {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = listOf(
                            OrderCommand.Create.OrderLineItem(productId = productId, quantity = 10),
                        ),
                    ),
                )
            }

            assertThat(exception.errors[0].reason).isEqualTo("INSUFFICIENT_STOCK")
        }

        @DisplayName("복합 오류(삭제 + 재고 부족) 시 오류 목록을 수집한다")
        @Test
        fun collectMultipleErrors() {
            val userId = registerUser()
            val brandId = registerBrand()
            val deletedProductId = registerProduct(brandId, name = "삭제 상품")
            val lowStockProductId = registerProduct(brandId, name = "재고 부족 상품", stock = 3)

            val deletedProduct = productRepository.findByIdOrNull(deletedProductId)!!
            deletedProduct.delete()
            productRepository.save(deletedProduct)

            val exception = assertThrows<OrderValidationException> {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = listOf(
                            OrderCommand.Create.OrderLineItem(productId = deletedProductId, quantity = 1),
                            OrderCommand.Create.OrderLineItem(productId = lowStockProductId, quantity = 10),
                        ),
                    ),
                )
            }

            assertThat(exception.errors).hasSize(2)
        }

        @DisplayName("중복 상품 시 검증 실패")
        @Test
        fun failWhenDuplicateProduct() {
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId)

            val exception = assertThrows<CoreException> {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = listOf(
                            OrderCommand.Create.OrderLineItem(productId = productId, quantity = 1),
                            OrderCommand.Create.OrderLineItem(productId = productId, quantity = 2),
                        ),
                    ),
                )
            }

            assertThat(exception.errorCode).isEqualTo(OrderErrorCode.DUPLICATE_ORDER_ITEM)
        }

        @DisplayName("빈 주문 시 검증 실패")
        @Test
        fun failWhenEmptyItems() {
            val userId = registerUser()

            val exception = assertThrows<CoreException> {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = emptyList(),
                    ),
                )
            }

            assertThat(exception.errorCode).isEqualTo(OrderErrorCode.EMPTY_ORDER_ITEMS)
        }
    }
}
