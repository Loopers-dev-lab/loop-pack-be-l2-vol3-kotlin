package com.loopers.application.user.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.domain.order.IdempotencyKey
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import java.math.BigDecimal

@DisplayName("OrderCreateUseCase")
class OrderCreateUseCaseTest {

    private val orderRepository: OrderRepository = mock()
    private val productRepository: ProductRepository = mock()
    private val productStockRepository: ProductStockRepository = mock()
    private val brandRepository: BrandRepository = mock()
    private val couponRepository: CouponRepository = mock()
    private val issuedCouponRepository: IssuedCouponRepository = mock()
    private val useCase = OrderCreateUseCase(
        orderRepository = orderRepository,
        productRepository = productRepository,
        productStockRepository = productStockRepository,
        brandRepository = brandRepository,
        couponRepository = couponRepository,
        issuedCouponRepository = issuedCouponRepository,
    )

    private fun command(
        userId: Long = 1L,
        idempotencyKey: String = "test-key-001",
        items: List<OrderCreateCommand.Item> = listOf(
            OrderCreateCommand.Item(productId = 1L, quantity = 2),
        ),
    ): OrderCreateCommand = OrderCreateCommand(userId, idempotencyKey, items)

    private fun product(
        id: Long = 1L,
        name: String = "테스트 상품",
        brandId: Long = 1L,
        status: Product.Status = Product.Status.ACTIVE,
        regularPrice: BigDecimal = BigDecimal("10000"),
        sellingPrice: BigDecimal = BigDecimal("8000"),
    ): Product = Product.retrieve(
        id = id,
        name = name,
        regularPrice = Money(regularPrice),
        sellingPrice = Money(sellingPrice),
        brandId = brandId,
        imageUrl = null,
        thumbnailUrl = null,
        likeCount = 0,
        status = status,
    )

    private fun brand(
        id: Long = 1L,
        name: String = "테스트 브랜드",
        status: Brand.Status = Brand.Status.ACTIVE,
    ): Brand = Brand.retrieve(id = id, name = name, status = status)

    private fun stock(
        id: Long = 1L,
        productId: Long = 1L,
        quantity: Int = 100,
    ): ProductStock = ProductStock.retrieve(id = id, productId = productId, quantity = Quantity(quantity))

    private fun savedOrder(order: Order): Order = Order.retrieve(
        id = 100L,
        userId = order.userId,
        idempotencyKey = order.idempotencyKey,
        status = order.status,
        items = order.items,
        createdAt = java.time.ZonedDateTime.now(),
    )

    private fun stubNormalFlow(
        products: List<Product> = listOf(product()),
        brands: List<Brand> = listOf(brand()),
        stocks: List<ProductStock> = listOf(stock()),
    ) {
        given(orderRepository.existsByIdempotencyKey(IdempotencyKey("test-key-001"))).willReturn(false)
        given(productRepository.findAllByIdIn(products.map { it.id!! })).willReturn(products)
        given(brandRepository.findAllByIdIn(brands.map { it.id!! })).willReturn(brands)
        given(productStockRepository.findAllByProductIdInWithLock(products.map { it.id!! })).willReturn(stocks)
        given(
            orderRepository.save(
                check { order ->
                    assertThat(order.status).isEqualTo(Order.Status.CREATED)
                    assertThat(order.userId).isEqualTo(1L)
                },
            ),
        ).willAnswer { savedOrder(it.arguments[0] as Order) }
        given(
            productStockRepository.saveAll(
                check { stocks ->
                    assertThat(stocks).isNotEmpty()
                },
            ),
        ).willAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            invocation.arguments[0] as List<ProductStock>
        }
    }

    @Nested
    @DisplayName("정상 주문 생성 시 OrderResult.Created를 반환한다")
    inner class WhenNormalOrder {

        @Test
        @DisplayName("단일 상품 주문 → Created(orderId, CREATED)")
        fun create_singleItem() {
            // arrange
            stubNormalFlow()

            // act
            val result = useCase.create(command())

            // assert
            assertAll(
                { assertThat(result.orderId).isEqualTo(100L) },
                { assertThat(result.status).isEqualTo("CREATED") },
            )
        }

        @Test
        @DisplayName("여러 상품 주문 성공")
        fun create_multipleItems() {
            // arrange
            val p1 = product(id = 1L, brandId = 1L)
            val p2 = product(id = 2L, name = "상품B", brandId = 2L)
            val b1 = brand(id = 1L)
            val b2 = brand(id = 2L, name = "브랜드B")
            val s1 = stock(id = 1L, productId = 1L)
            val s2 = stock(id = 2L, productId = 2L)

            given(orderRepository.existsByIdempotencyKey(IdempotencyKey("test-key-001"))).willReturn(false)
            given(productRepository.findAllByIdIn(listOf(1L, 2L))).willReturn(listOf(p1, p2))
            given(brandRepository.findAllByIdIn(listOf(1L, 2L))).willReturn(listOf(b1, b2))
            given(productStockRepository.findAllByProductIdInWithLock(listOf(1L, 2L))).willReturn(listOf(s1, s2))
            given(
                orderRepository.save(
                    check { order ->
                        assertThat(order.status).isEqualTo(Order.Status.CREATED)
                        assertThat(order.items).hasSize(2)
                    },
                ),
            ).willAnswer { savedOrder(it.arguments[0] as Order) }
            given(
                productStockRepository.saveAll(
                    check { stocks ->
                        assertThat(stocks).hasSize(2)
                    },
                ),
            ).willAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                invocation.arguments[0] as List<ProductStock>
            }

            // act
            val result = useCase.create(
                command(
                    items = listOf(
                        OrderCreateCommand.Item(productId = 1L, quantity = 2),
                        OrderCreateCommand.Item(productId = 2L, quantity = 3),
                    ),
                ),
            )

            // assert
            assertThat(result.orderId).isEqualTo(100L)
        }
    }

    @Nested
    @DisplayName("동일 상품 중복 요청 시 수량을 합산하여 정상 생성한다")
    inner class WhenDuplicateProduct {

        @Test
        @DisplayName("productId=1이 2건(수량3, 수량2) → 합산 수량5로 주문")
        fun create_mergesDuplicateProducts() {
            // arrange
            stubNormalFlow()

            // act
            useCase.create(
                command(
                    items = listOf(
                        OrderCreateCommand.Item(productId = 1L, quantity = 3),
                        OrderCreateCommand.Item(productId = 1L, quantity = 2),
                    ),
                ),
            )

            // assert
            then(orderRepository).should().save(
                check { order ->
                    assertAll(
                        { assertThat(order.items).hasSize(1) },
                        { assertThat(order.items[0].quantity.value).isEqualTo(5) },
                    )
                },
            )
        }
    }

    @Nested
    @DisplayName("멱등성 키가 중복이면 ORDER_IDEMPOTENCY_KEY_DUPLICATE 예외를 던진다")
    inner class WhenDuplicateIdempotencyKey {

        @Test
        @DisplayName("이미 존재하는 idempotencyKey → 예외")
        fun create_duplicateKey() {
            // arrange
            given(orderRepository.existsByIdempotencyKey(IdempotencyKey("test-key-001"))).willReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.ORDER_IDEMPOTENCY_KEY_DUPLICATE)
        }
    }

    @Nested
    @DisplayName("존재하지 않는 상품이 포함되면 PRODUCT_NOT_FOUND 예외를 던진다")
    inner class WhenProductNotFound {

        @Test
        @DisplayName("요청 productId 중 DB에 없는 것이 있으면 → 예외")
        fun create_productNotFound() {
            // arrange
            given(orderRepository.existsByIdempotencyKey(IdempotencyKey("test-key-001"))).willReturn(false)
            given(productRepository.findAllByIdIn(listOf(1L))).willReturn(emptyList())

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("INACTIVE 상품이 포함되면 PRODUCT_NOT_FOUND 예외를 던진다")
    inner class WhenProductInactive {

        @Test
        @DisplayName("INACTIVE 상품 → 예외")
        fun create_inactiveProduct() {
            // arrange
            given(orderRepository.existsByIdempotencyKey(IdempotencyKey("test-key-001"))).willReturn(false)
            given(productRepository.findAllByIdIn(listOf(1L)))
                .willReturn(listOf(product(status = Product.Status.INACTIVE)))

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("브랜드가 INACTIVE이면 PRODUCT_NOT_FOUND 예외를 던진다")
    inner class WhenBrandInactive {

        @Test
        @DisplayName("INACTIVE 브랜드 → 예외 (노출 원칙)")
        fun create_inactiveBrand() {
            // arrange
            given(orderRepository.existsByIdempotencyKey(IdempotencyKey("test-key-001"))).willReturn(false)
            given(productRepository.findAllByIdIn(listOf(1L))).willReturn(listOf(product()))
            given(brandRepository.findAllByIdIn(listOf(1L)))
                .willReturn(listOf(brand(status = Brand.Status.INACTIVE)))

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("상품의 재고 정보가 없으면 PRODUCT_STOCK_NOT_FOUND 예외를 던진다")
    inner class WhenStockNotFound {

        @Test
        @DisplayName("재고 정보 없는 상품 → 예외")
        fun create_stockNotFound() {
            // arrange
            given(orderRepository.existsByIdempotencyKey(IdempotencyKey("test-key-001"))).willReturn(false)
            given(productRepository.findAllByIdIn(listOf(1L))).willReturn(listOf(product()))
            given(brandRepository.findAllByIdIn(listOf(1L))).willReturn(listOf(brand()))
            given(productStockRepository.findAllByProductIdInWithLock(listOf(1L))).willReturn(emptyList())

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_STOCK_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("브랜드가 DB에 없으면 PRODUCT_NOT_FOUND 예외를 던진다")
    inner class WhenBrandNotFound {

        @Test
        @DisplayName("브랜드 조회 결과 없음 → 예외")
        fun create_brandNotFound() {
            // arrange
            given(orderRepository.existsByIdempotencyKey(IdempotencyKey("test-key-001"))).willReturn(false)
            given(productRepository.findAllByIdIn(listOf(1L))).willReturn(listOf(product()))
            given(brandRepository.findAllByIdIn(listOf(1L))).willReturn(emptyList())

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("주문 수량이 0 이하이면 INVALID_QUANTITY 예외를 던진다")
    inner class WhenInvalidQuantity {

        @Test
        @DisplayName("수량 0 → 예외")
        fun create_zeroQuantity() {
            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command(items = listOf(OrderCreateCommand.Item(productId = 1L, quantity = 0))))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.INVALID_QUANTITY)
        }

        @Test
        @DisplayName("음수 수량 → 예외")
        fun create_negativeQuantity() {
            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command(items = listOf(OrderCreateCommand.Item(productId = 1L, quantity = -1))))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.INVALID_QUANTITY)
        }
    }

    @Nested
    @DisplayName("재고가 부족하면 PRODUCT_STOCK_INSUFFICIENT 예외를 던진다")
    inner class WhenStockInsufficient {

        @Test
        @DisplayName("재고 1개, 주문 2개 → 예외")
        fun create_insufficientStock() {
            // arrange
            given(orderRepository.existsByIdempotencyKey(IdempotencyKey("test-key-001"))).willReturn(false)
            given(productRepository.findAllByIdIn(listOf(1L))).willReturn(listOf(product()))
            given(brandRepository.findAllByIdIn(listOf(1L))).willReturn(listOf(brand()))
            given(productStockRepository.findAllByProductIdInWithLock(listOf(1L)))
                .willReturn(listOf(stock(quantity = 1)))

            // act
            val exception = assertThrows<CoreException> {
                useCase.create(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_STOCK_INSUFFICIENT)
        }
    }

    @Nested
    @DisplayName("주문 성공 시 재고가 차감된다 (부수효과)")
    inner class WhenStockDecreased {

        @Test
        @DisplayName("재고 100, 주문 2 → 차감된 재고(98) 저장 호출")
        fun create_stockDecreased() {
            // arrange
            stubNormalFlow()

            // act
            useCase.create(command())

            // assert
            then(productStockRepository).should().saveAll(
                check { stocks ->
                    assertAll(
                        { assertThat(stocks).hasSize(1) },
                        { assertThat(stocks[0].quantity.value).isEqualTo(98) },
                    )
                },
            )
        }
    }

    @Nested
    @DisplayName("주문 성공 시 Order와 ProductStock이 모두 저장된다")
    inner class WhenOrderAndStockSaved {

        @Test
        @DisplayName("orderRepository.save()와 productStockRepository.saveAll() 호출 확인")
        fun create_bothSaved() {
            // arrange
            stubNormalFlow()

            // act
            useCase.create(command())

            // assert
            then(orderRepository).should().save(
                check { order ->
                    assertAll(
                        { assertThat(order.userId).isEqualTo(1L) },
                        { assertThat(order.status).isEqualTo(Order.Status.CREATED) },
                        { assertThat(order.items).hasSize(1) },
                    )
                },
            )
            then(productStockRepository).should().saveAll(
                check { stocks -> assertThat(stocks).hasSize(1) },
            )
        }
    }
}
