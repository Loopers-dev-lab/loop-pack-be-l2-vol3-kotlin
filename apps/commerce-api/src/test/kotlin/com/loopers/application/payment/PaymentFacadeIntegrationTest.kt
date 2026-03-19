package com.loopers.application.payment

import com.loopers.application.order.OrderFacade
import com.loopers.application.order.OrderItemCriteria
import com.loopers.domain.brand.Brand
import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.product.Product
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.infrastructure.pg.PgApiResponse
import com.loopers.infrastructure.pg.PgApiResponse.PgMeta
import com.loopers.infrastructure.pg.PgPaymentResponse
import com.loopers.infrastructure.pg.PgCallbackRequest
import com.loopers.infrastructure.pg.PgTransactionDetailResponse
import com.loopers.infrastructure.payment.PaymentJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.ZonedDateTime

/**
 * PaymentFacade 통합 테스트
 * - 실제 DB(TestContainers)와 연동하여 Facade → Service → Repository 레이어 통합 테스트
 * - PgPaymentClient만 MockBean으로 교체 (외부 PG 의존 제거)
 * - Order→Payment 생성→PG 호출→콜백→상태 변경→보상 흐름 검증
 */
@SpringBootTest
class PaymentFacadeIntegrationTest @Autowired constructor(
    private val paymentFacade: PaymentFacade,
    private val orderFacade: OrderFacade,
    private val paymentJpaRepository: PaymentJpaRepository,
    private val orderJpaRepository: OrderJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @MockitoBean
    private lateinit var pgPaymentClient: PgPaymentClient

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand(name: String = "나이키"): Brand {
        return brandJpaRepository.save(Brand(name = name, description = "스포츠 브랜드"))
    }

    private fun createProduct(
        brand: Brand = createBrand(),
        name: String = "에어맥스 90",
        price: BigDecimal = BigDecimal("129000"),
        stock: Int = 100,
    ): Product {
        return productJpaRepository.save(
            Product(
                brandId = brand.id,
                name = name,
                price = price,
                stock = stock,
                description = null,
                imageUrl = null,
            ),
        )
    }

    private fun createCoupon(
        type: CouponType = CouponType.FIXED,
        value: BigDecimal = BigDecimal("5000"),
        minOrderAmount: BigDecimal? = BigDecimal("10000"),
    ): Coupon {
        return couponJpaRepository.save(
            Coupon(
                name = "테스트 쿠폰",
                type = type,
                value = value,
                minOrderAmount = minOrderAmount,
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        )
    }

    private fun issueCoupon(couponId: Long, userId: Long): IssuedCoupon {
        return issuedCouponJpaRepository.save(IssuedCoupon(couponId = couponId, userId = userId))
    }

    private fun createOrderWithItems(
        userId: Long,
        productId: Long,
        quantity: Int,
        couponId: Long? = null,
    ): Long {
        val criteria = listOf(OrderItemCriteria(productId = productId, quantity = quantity))
        val orderInfo = orderFacade.createOrder(userId = userId, criteria = criteria, couponId = couponId)
        return orderInfo.id
    }

    private fun mockPgSuccess(transactionKey: String = "txn-12345") {
        whenever(pgPaymentClient.requestPayment(any(), any())).thenReturn(
            PgApiResponse(
                meta = PgMeta(result = "SUCCESS", errorCode = null, message = null),
                data = PgPaymentResponse(transactionKey = transactionKey, status = "SUCCESS", reason = null),
            ),
        )
    }

    private fun mockPgFailure() {
        whenever(pgPaymentClient.requestPayment(any(), any()))
            .thenThrow(RuntimeException("PG 시스템 장애"))
    }

    @DisplayName("결제 요청 시,")
    @Nested
    inner class RequestPayment {

        @DisplayName("PG 결제가 성공하면, Payment가 REQUESTED 상태로 저장되고 transactionKey가 기록된다.")
        @Test
        fun createsPaymentAndMarksRequested_whenPgSuccess() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand = brand, stock = 100)
            val orderId = createOrderWithItems(userId, product.id, 2)
            mockPgSuccess("txn-abc-123")

            // act
            val result = paymentFacade.requestPayment(userId, PaymentCriteria(orderId, "VISA", "1234-5678-9012-3456"))

            // assert
            val savedPayment = paymentJpaRepository.findByOrderId(orderId)!!
            val savedOrder = orderJpaRepository.findByIdWithItems(orderId)!!
            assertAll(
                { assertThat(result.status).isEqualTo(PaymentStatus.REQUESTED) },
                { assertThat(savedPayment.status).isEqualTo(PaymentStatus.REQUESTED) },
                { assertThat(savedPayment.transactionKey).isEqualTo("txn-abc-123") },
                { assertThat(savedOrder.status).isEqualTo(OrderStatus.PENDING) },
            )
        }

        @DisplayName("PG 호출이 실패하면, Payment가 FAILED 상태가 되고 재고·쿠폰이 복원된다.")
        @Test
        fun marksFailedAndCompensates_whenPgFails() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand = brand, stock = 100)
            val coupon = createCoupon()
            val issuedCoupon = issueCoupon(coupon.id, userId)
            val orderId = createOrderWithItems(userId, product.id, 2, couponId = issuedCoupon.id)
            mockPgFailure()

            // act
            val result = paymentFacade.requestPayment(userId, PaymentCriteria(orderId, "VISA", "1234-5678-9012-3456"))

            // assert
            val savedPayment = paymentJpaRepository.findByOrderId(orderId)!!
            val savedOrder = orderJpaRepository.findByIdWithItems(orderId)!!
            val restoredProduct = productJpaRepository.findById(product.id).get()
            val restoredCoupon = issuedCouponJpaRepository.findById(issuedCoupon.id).get()
            assertAll(
                { assertThat(result.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(savedPayment.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(savedOrder.status).isEqualTo(OrderStatus.FAILED) },
                { assertThat(restoredProduct.stock).isEqualTo(100) },
                { assertThat(restoredCoupon.status.name).isEqualTo("AVAILABLE") },
            )
        }

        @DisplayName("이미 결제가 진행 중인 주문이면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenDuplicatePayment() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand = brand, stock = 100)
            val orderId = createOrderWithItems(userId, product.id, 2)
            mockPgSuccess("txn-first")
            paymentFacade.requestPayment(userId, PaymentCriteria(orderId, "VISA", "1234-5678-9012-3456"))

            // act & assert
            val exception = assertThrows<CoreException> {
                paymentFacade.requestPayment(userId, PaymentCriteria(orderId, "VISA", "1234-5678-9012-3456"))
            }
            assertAll(
                { assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT) },
                { assertThat(paymentJpaRepository.findAll()).hasSize(1) },
            )
        }

        @DisplayName("다른 사용자의 주문에 결제를 요청하면, FORBIDDEN 예외가 발생한다.")
        @Test
        fun throwsForbidden_whenNotOwner() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand = brand, stock = 100)
            val orderId = createOrderWithItems(1L, product.id, 2)

            // act & assert
            val exception = assertThrows<CoreException> {
                paymentFacade.requestPayment(999L, PaymentCriteria(orderId, "VISA", "1234-5678-9012-3456"))
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }

        @DisplayName("결제 가능한 상태가 아닌 주문이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenOrderNotPayable() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand = brand, stock = 100)
            val orderId = createOrderWithItems(userId, product.id, 2)

            // 주문을 PAID 상태로 변경
            val order = orderJpaRepository.findByIdWithItems(orderId)!!
            order.markPaid()
            orderJpaRepository.save(order)

            // act & assert
            val exception = assertThrows<CoreException> {
                paymentFacade.requestPayment(userId, PaymentCriteria(orderId, "VISA", "1234-5678-9012-3456"))
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("콜백 처리 시,")
    @Nested
    inner class HandleCallback {

        @DisplayName("SUCCESS 콜백이 오면, DB에서 Payment=PAID, Order=PAID 상태가 된다.")
        @Test
        fun marksPaidInDb_whenSuccessCallback() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand = brand, stock = 100)
            val orderId = createOrderWithItems(userId, product.id, 2)
            mockPgSuccess("txn-callback-success")
            paymentFacade.requestPayment(userId, PaymentCriteria(orderId, "VISA", "1234-5678-9012-3456"))

            val callbackRequest = PgCallbackRequest(
                transactionKey = "txn-callback-success",
                orderId = orderId.toString(),
                cardType = "VISA",
                cardNo = "1234-5678-9012-3456",
                amount = 258000,
                status = "SUCCESS",
                reason = null,
            )

            // act
            paymentFacade.handleCallback(callbackRequest)

            // assert
            val savedPayment = paymentJpaRepository.findByOrderId(orderId)!!
            val savedOrder = orderJpaRepository.findByIdWithItems(orderId)!!
            assertAll(
                { assertThat(savedPayment.status).isEqualTo(PaymentStatus.PAID) },
                { assertThat(savedOrder.status).isEqualTo(OrderStatus.PAID) },
            )
        }

        @DisplayName("FAILED 콜백이 오면, DB에서 Payment=FAILED, Order=FAILED 상태가 되고 재고·쿠폰이 복원된다.")
        @Test
        fun marksFailedAndCompensatesInDb_whenFailedCallback() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand = brand, stock = 100)
            val coupon = createCoupon()
            val issuedCoupon = issueCoupon(coupon.id, userId)
            val orderId = createOrderWithItems(userId, product.id, 2, couponId = issuedCoupon.id)
            mockPgSuccess("txn-callback-fail")
            paymentFacade.requestPayment(userId, PaymentCriteria(orderId, "VISA", "1234-5678-9012-3456"))

            val callbackRequest = PgCallbackRequest(
                transactionKey = "txn-callback-fail",
                orderId = orderId.toString(),
                cardType = "VISA",
                cardNo = "1234-5678-9012-3456",
                amount = 258000,
                status = "FAILED",
                reason = "잔액 부족",
            )

            // act
            paymentFacade.handleCallback(callbackRequest)

            // assert
            val savedPayment = paymentJpaRepository.findByOrderId(orderId)!!
            val savedOrder = orderJpaRepository.findByIdWithItems(orderId)!!
            val restoredProduct = productJpaRepository.findById(product.id).get()
            val restoredCoupon = issuedCouponJpaRepository.findById(issuedCoupon.id).get()
            assertAll(
                { assertThat(savedPayment.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(savedPayment.failReason).isEqualTo("잔액 부족") },
                { assertThat(savedOrder.status).isEqualTo(OrderStatus.FAILED) },
                { assertThat(restoredProduct.stock).isEqualTo(100) },
                { assertThat(restoredCoupon.status.name).isEqualTo("AVAILABLE") },
            )
        }

        @DisplayName("이미 PAID 상태인 결제에 콜백이 오면, 상태가 변하지 않는다.")
        @Test
        fun ignoresCallback_whenAlreadyPaid() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand = brand, stock = 100)
            val orderId = createOrderWithItems(userId, product.id, 2)
            mockPgSuccess("txn-already-paid")
            paymentFacade.requestPayment(userId, PaymentCriteria(orderId, "VISA", "1234-5678-9012-3456"))

            // 먼저 SUCCESS 콜백으로 PAID 처리
            paymentFacade.handleCallback(
                PgCallbackRequest(
                    transactionKey = "txn-already-paid",
                    orderId = orderId.toString(),
                    cardType = "VISA",
                    cardNo = "1234-5678-9012-3456",
                    amount = 258000,
                    status = "SUCCESS",
                    reason = null,
                ),
            )

            // act - 중복 콜백
            paymentFacade.handleCallback(
                PgCallbackRequest(
                    transactionKey = "txn-already-paid",
                    orderId = orderId.toString(),
                    cardType = "VISA",
                    cardNo = "1234-5678-9012-3456",
                    amount = 258000,
                    status = "FAILED",
                    reason = "중복 콜백",
                ),
            )

            // assert
            val savedPayment = paymentJpaRepository.findByOrderId(orderId)!!
            val savedOrder = orderJpaRepository.findByIdWithItems(orderId)!!
            assertAll(
                { assertThat(savedPayment.status).isEqualTo(PaymentStatus.PAID) },
                { assertThat(savedOrder.status).isEqualTo(OrderStatus.PAID) },
            )
        }
    }

    @DisplayName("상태 조회 시,")
    @Nested
    inner class GetPaymentStatus {

        @DisplayName("REQUESTED 상태에서 PG가 SUCCESS를 반환하면, DB에서 Payment=PAID, Order=PAID가 된다.")
        @Test
        fun syncsFromPgAndMarksPaid_whenRequestedAndPgSuccess() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand = brand, stock = 100)
            val orderId = createOrderWithItems(userId, product.id, 2)
            mockPgSuccess("txn-sync-success")
            paymentFacade.requestPayment(userId, PaymentCriteria(orderId, "VISA", "1234-5678-9012-3456"))

            whenever(pgPaymentClient.getPaymentStatus(eq(userId.toString()), eq("txn-sync-success"))).thenReturn(
                PgApiResponse(
                    meta = PgMeta(result = "SUCCESS", errorCode = null, message = null),
                    data = PgTransactionDetailResponse(
                        transactionKey = "txn-sync-success",
                        orderId = orderId.toString(),
                        cardType = "VISA",
                        cardNo = "1234-5678-9012-3456",
                        amount = 258000,
                        status = "SUCCESS",
                        reason = null,
                    ),
                ),
            )

            // act
            val result = paymentFacade.getPaymentStatus(userId, orderId)

            // assert
            val savedPayment = paymentJpaRepository.findByOrderId(orderId)!!
            val savedOrder = orderJpaRepository.findByIdWithItems(orderId)!!
            assertAll(
                { assertThat(result.status).isEqualTo(PaymentStatus.PAID) },
                { assertThat(savedPayment.status).isEqualTo(PaymentStatus.PAID) },
                { assertThat(savedOrder.status).isEqualTo(OrderStatus.PAID) },
            )
        }

        @DisplayName("REQUESTED 상태에서 PG가 FAILED를 반환하면, DB에서 Payment=FAILED, Order=FAILED가 되고 보상 처리된다.")
        @Test
        fun syncsFromPgAndCompensates_whenRequestedAndPgFailed() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand = brand, stock = 100)
            val coupon = createCoupon()
            val issuedCoupon = issueCoupon(coupon.id, userId)
            val orderId = createOrderWithItems(userId, product.id, 2, couponId = issuedCoupon.id)
            mockPgSuccess("txn-sync-fail")
            paymentFacade.requestPayment(userId, PaymentCriteria(orderId, "VISA", "1234-5678-9012-3456"))

            whenever(pgPaymentClient.getPaymentStatus(eq(userId.toString()), eq("txn-sync-fail"))).thenReturn(
                PgApiResponse(
                    meta = PgMeta(result = "SUCCESS", errorCode = null, message = null),
                    data = PgTransactionDetailResponse(
                        transactionKey = "txn-sync-fail",
                        orderId = orderId.toString(),
                        cardType = "VISA",
                        cardNo = "1234-5678-9012-3456",
                        amount = 258000,
                        status = "FAILED",
                        reason = "카드 한도 초과",
                    ),
                ),
            )

            // act
            val result = paymentFacade.getPaymentStatus(userId, orderId)

            // assert
            val savedPayment = paymentJpaRepository.findByOrderId(orderId)!!
            val savedOrder = orderJpaRepository.findByIdWithItems(orderId)!!
            val restoredProduct = productJpaRepository.findById(product.id).get()
            val restoredCoupon = issuedCouponJpaRepository.findById(issuedCoupon.id).get()
            assertAll(
                { assertThat(result.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(savedPayment.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(savedOrder.status).isEqualTo(OrderStatus.FAILED) },
                { assertThat(restoredProduct.stock).isEqualTo(100) },
                { assertThat(restoredCoupon.status.name).isEqualTo("AVAILABLE") },
            )
        }

        @DisplayName("이미 PAID 상태이면, PG를 호출하지 않고 현재 상태를 반환한다.")
        @Test
        fun returnsCurrentStatus_whenAlreadyPaid() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand = brand, stock = 100)
            val orderId = createOrderWithItems(userId, product.id, 2)
            mockPgSuccess("txn-already-done")
            paymentFacade.requestPayment(userId, PaymentCriteria(orderId, "VISA", "1234-5678-9012-3456"))

            // SUCCESS 콜백으로 PAID 처리
            paymentFacade.handleCallback(
                PgCallbackRequest(
                    transactionKey = "txn-already-done",
                    orderId = orderId.toString(),
                    cardType = "VISA",
                    cardNo = "1234-5678-9012-3456",
                    amount = 258000,
                    status = "SUCCESS",
                    reason = null,
                ),
            )

            // act
            val result = paymentFacade.getPaymentStatus(userId, orderId)

            // assert
            assertAll(
                { assertThat(result.status).isEqualTo(PaymentStatus.PAID) },
                { assertThat(result.transactionKey).isEqualTo("txn-already-done") },
            )
        }
    }
}
