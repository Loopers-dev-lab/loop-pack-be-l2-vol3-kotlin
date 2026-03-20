package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PgPaymentStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PgLookupRetryOnUnavailablePredicateTest {

    private val predicate = PgLookupRetryOnUnavailablePredicate()

    @Test
    fun `PG 조회 결과가 Unavailable이면 재시도 대상으로 판단한다`() {
        val result = PaymentGateway.LookupResult.Unavailable("PG 상태 조회 중 타임아웃이 발생했습니다.")

        assertThat(predicate.test(result)).isTrue()
    }

    @Test
    fun `PG 조회 결과가 Found면 재시도하지 않는다`() {
        val result = PaymentGateway.LookupResult.Found(
            transactionKey = "20250816:TR:9577c5",
            status = PgPaymentStatus.SUCCESS,
            reason = null,
        )

        assertThat(predicate.test(result)).isFalse()
    }

    @Test
    fun `PG 조회 결과가 NotFound면 재시도하지 않는다`() {
        assertThat(predicate.test(PaymentGateway.LookupResult.NotFound)).isFalse()
    }
}
