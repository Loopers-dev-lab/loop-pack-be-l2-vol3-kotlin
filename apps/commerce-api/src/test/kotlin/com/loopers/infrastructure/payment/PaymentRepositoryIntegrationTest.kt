package com.loopers.infrastructure.payment

import com.loopers.domain.common.Money
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentIdempotencyKey
import com.loopers.domain.payment.PaymentReasonCode
import com.loopers.domain.payment.PaymentRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@DisplayName("PaymentRepository 통합 테스트")
@SpringBootTest
class PaymentRepositoryIntegrationTest
@Autowired
constructor(
    private val paymentRepository: PaymentRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createPayment(
        orderId: Long = 1L,
        userId: Long = 1L,
        idempotencyKey: String = "pay-key-001",
        requestFingerprint: String = "fp-001",
    ): Payment = Payment.create(
        orderId = orderId,
        userId = userId,
        idempotencyKey = PaymentIdempotencyKey(idempotencyKey),
        cardType = "VISA",
        maskedCardNo = "****1234",
        amount = Money(BigDecimal("10000")),
        requestFingerprint = requestFingerprint,
    )

    @Nested
    @DisplayName("save + findById 시")
    inner class WhenSaveAndFindById {

        @Test
        @DisplayName("Payment가 저장되고 조회된다")
        fun save_findById() {
            val saved = paymentRepository.save(createPayment())

            val found = paymentRepository.findById(saved.id!!)

            assertThat(found).isNotNull
            assertAll(
                { assertThat(found!!.id).isEqualTo(saved.id) },
                { assertThat(found!!.orderId).isEqualTo(1L) },
                { assertThat(found!!.status).isEqualTo(Payment.Status.PENDING) },
                { assertThat(found!!.transactionKey).isNull() },
                { assertThat(found!!.createdAt).isNotNull() },
            )
        }
    }

    @Nested
    @DisplayName("findByIdempotencyKey 시")
    inner class WhenFindByIdempotencyKey {

        @Test
        @DisplayName("존재하는 멱등키로 조회 성공")
        fun findByIdempotencyKey_found() {
            paymentRepository.save(createPayment(idempotencyKey = "unique-key"))

            val found = paymentRepository.findByIdempotencyKey(PaymentIdempotencyKey("unique-key"))

            assertThat(found).isNotNull
            assertThat(found!!.idempotencyKey.value).isEqualTo("unique-key")
        }

        @Test
        @DisplayName("존재하지 않는 멱등키는 null")
        fun findByIdempotencyKey_notFound() {
            val found = paymentRepository.findByIdempotencyKey(PaymentIdempotencyKey("not-exist"))

            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("findActiveByOrderId 시")
    inner class WhenFindActiveByOrderId {

        @Test
        @DisplayName("PENDING 상태의 Payment만 조회된다")
        fun findActiveByOrderId_pendingOnly() {
            paymentRepository.save(createPayment(orderId = 1L, idempotencyKey = "key-1"))

            val found = paymentRepository.findActiveByOrderId(1L)

            assertThat(found).isNotNull
            assertThat(found!!.status).isEqualTo(Payment.Status.PENDING)
        }

        @Test
        @DisplayName("FAILED Payment만 있으면 null")
        fun findActiveByOrderId_failedOnly() {
            val saved = paymentRepository.save(createPayment(orderId = 2L, idempotencyKey = "key-2"))
            val failed = saved.fail(PaymentReasonCode.PG_INTERNAL_ERROR)
            paymentRepository.save(failed)

            val found = paymentRepository.findActiveByOrderId(2L)

            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("save로 상태 업데이트 시")
    inner class WhenUpdateStatus {

        @Test
        @DisplayName("PENDING → SUCCESS 업데이트")
        fun save_updateToSuccess() {
            val saved = paymentRepository.save(createPayment(idempotencyKey = "update-key"))
            val succeeded = saved.succeed("txn-key-001")

            val updated = paymentRepository.save(succeeded)

            assertAll(
                { assertThat(updated.id).isEqualTo(saved.id) },
                { assertThat(updated.status).isEqualTo(Payment.Status.SUCCESS) },
                { assertThat(updated.transactionKey).isEqualTo("txn-key-001") },
            )
        }
    }

    @Nested
    @DisplayName("findPendingOlderThan 시")
    inner class WhenFindPendingOlderThan {

        @Test
        @DisplayName("threshold 이전에 생성된 PENDING Payment만 조회된다")
        fun findPendingOlderThan_found() {
            paymentRepository.save(createPayment(idempotencyKey = "old-key"))

            val results = paymentRepository.findPendingOlderThan(
                java.time.ZonedDateTime.now().plusMinutes(1),
            )

            assertThat(results).isNotEmpty
        }

        @Test
        @DisplayName("threshold 이후에 생성된 Payment는 조회되지 않는다")
        fun findPendingOlderThan_notFound() {
            paymentRepository.save(createPayment(idempotencyKey = "new-key"))

            val results = paymentRepository.findPendingOlderThan(
                java.time.ZonedDateTime.now().minusMinutes(10),
            )

            assertThat(results).isEmpty()
        }
    }
}
