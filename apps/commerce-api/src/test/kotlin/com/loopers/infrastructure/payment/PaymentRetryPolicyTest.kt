package com.loopers.infrastructure.payment

import io.github.resilience4j.retry.RetryRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@DisplayName("결제 Retry 정책")
class PaymentRetryPolicyTest @Autowired constructor(
    private val retryRegistry: RetryRegistry,
) {

    @DisplayName("pg-simulator에 retry가 설정되어 있으면 비멱등 결제 요청도 재시도되어 이중 결제 위험이 있다")
    @Test
    fun paymentRequestShouldNotBeRetried() {
        val retryOptional = retryRegistry.find("pg-simulator")
        assertThat(retryOptional).isEmpty
    }

    @DisplayName("PG 상태 조회는 멱등하므로 retry가 적용되어야 한다")
    @Test
    fun transactionQueryShouldBeRetried() {
        val retryOptional = retryRegistry.find("pg-query")
        assertThat(retryOptional).isPresent
    }
}
