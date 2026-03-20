package com.loopers.infrastructure.payment

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.loopers.domain.payment.PaymentReasonCode
import com.loopers.domain.payment.PgPaymentRequest
import com.loopers.domain.payment.PgPaymentResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import java.math.BigDecimal

@DisplayName("PgPaymentAdapter")
class PgPaymentAdapterTest {

    companion object {
        private lateinit var wireMockServer: WireMockServer
        private lateinit var adapter: PgPaymentAdapter

        @JvmStatic
        @BeforeAll
        fun setup() {
            wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
            wireMockServer.start()

            val restClient = RestClient.builder()
                .baseUrl("http://localhost:${wireMockServer.port()}")
                .build()
            adapter = PgPaymentAdapter(restClient)
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            wireMockServer.stop()
        }
    }

    private fun pgRequest(
        userId: Long = 1L,
        orderId: Long = 100L,
        cardType: String = "SAMSUNG",
        cardNo: String = "1234-5678-9012-3456",
        amount: BigDecimal = BigDecimal("16000"),
        callbackUrl: String = "http://localhost:8080/webhook/v1/payments/200",
    ): PgPaymentRequest = PgPaymentRequest(
        userId = userId,
        orderId = orderId,
        cardType = cardType,
        cardNo = cardNo,
        amount = amount,
        callbackUrl = callbackUrl,
    )

    @Nested
    @DisplayName("결제 요청")
    inner class RequestPayment {

        @Nested
        @DisplayName("PG가 정상 응답하면 Accepted를 반환한다")
        inner class WhenPgSuccess {

            @Test
            @DisplayName("200 + transactionKey → Accepted(transactionKey)")
            fun requestPayment_accepted() {
                // arrange
                wireMockServer.stubFor(
                    post(urlPathEqualTo("/api/v1/payments"))
                        .withHeader("X-USER-ID", equalTo("1"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody(
                                    """
                                    {
                                        "meta": {"result": "SUCCESS", "errorCode": null, "message": null},
                                        "data": {"transactionKey": "txn-abc-123", "status": "PENDING", "reason": null}
                                    }
                                    """.trimIndent(),
                                ),
                        ),
                )

                // act
                val response = adapter.requestPayment(pgRequest())

                // assert
                assertThat(response).isInstanceOf(PgPaymentResponse.Accepted::class.java)
                assertThat((response as PgPaymentResponse.Accepted).transactionKey).isEqualTo("txn-abc-123")
            }
        }

        @Nested
        @DisplayName("PG가 500을 반환하면 ImmediateFailure를 반환한다")
        inner class WhenPgServerError {

            @Test
            @DisplayName("500 → ImmediateFailure(PG_INTERNAL_ERROR)")
            fun requestPayment_immediateFailure() {
                // arrange
                wireMockServer.stubFor(
                    post(urlPathEqualTo("/api/v1/payments"))
                        .withHeader("X-USER-ID", equalTo("1"))
                        .willReturn(
                            aResponse()
                                .withStatus(500)
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody(
                                    """
                                    {
                                        "meta": {"result": "FAIL", "errorCode": "INTERNAL_ERROR", "message": "서버 에러"},
                                        "data": null
                                    }
                                    """.trimIndent(),
                                ),
                        ),
                )

                // act
                val response = adapter.requestPayment(pgRequest())

                // assert
                assertThat(response).isInstanceOf(PgPaymentResponse.ImmediateFailure::class.java)
                assertThat((response as PgPaymentResponse.ImmediateFailure).reasonCode)
                    .isEqualTo(PaymentReasonCode.PG_INTERNAL_ERROR)
            }
        }
    }

    @Nested
    @DisplayName("결제 상태 조회")
    inner class QueryPaymentStatus {

        @Nested
        @DisplayName("PG에서 상태를 정상 반환하면 PgPaymentStatusResponse를 반환한다")
        inner class WhenQuerySuccess {

            @Test
            @DisplayName("SUCCESS 상태 조회 → transactionKey, status, reason 반환")
            fun queryPaymentStatus_success() {
                // arrange
                wireMockServer.stubFor(
                    get(urlEqualTo("/api/v1/payments/txn-abc-123"))
                        .withHeader("X-USER-ID", equalTo("1"))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody(
                                    """
                                    {
                                        "meta": {"result": "SUCCESS", "errorCode": null, "message": null},
                                        "data": {
                                            "transactionKey": "txn-abc-123",
                                            "orderId": "100",
                                            "cardType": "SAMSUNG",
                                            "cardNo": "1234-5678-9012-3456",
                                            "amount": 16000,
                                            "status": "SUCCESS",
                                            "reason": "정상 승인되었습니다."
                                        }
                                    }
                                    """.trimIndent(),
                                ),
                        ),
                )

                // act
                val response = adapter.queryPaymentStatus("txn-abc-123", 1L)

                // assert
                assertAll(
                    { assertThat(response.transactionKey).isEqualTo("txn-abc-123") },
                    { assertThat(response.status).isEqualTo("SUCCESS") },
                    { assertThat(response.reason).isEqualTo("정상 승인되었습니다.") },
                )
            }
        }
    }

    @Nested
    @DisplayName("reason code 매핑")
    inner class MapReasonCode {

        @Test
        @DisplayName("한도초과 → LIMIT_EXCEEDED")
        fun mapReasonCode_limitExceeded() {
            assertThat(adapter.mapReasonCode("한도초과입니다. 다른 카드를 선택해주세요."))
                .isEqualTo(PaymentReasonCode.LIMIT_EXCEEDED)
        }

        @Test
        @DisplayName("잘못된 카드 → INVALID_CARD")
        fun mapReasonCode_invalidCard() {
            assertThat(adapter.mapReasonCode("잘못된 카드입니다. 다른 카드를 선택해주세요."))
                .isEqualTo(PaymentReasonCode.INVALID_CARD)
        }

        @Test
        @DisplayName("null → PG_INTERNAL_ERROR")
        fun mapReasonCode_null() {
            assertThat(adapter.mapReasonCode(null))
                .isEqualTo(PaymentReasonCode.PG_INTERNAL_ERROR)
        }

        @Test
        @DisplayName("알 수 없는 사유 → PG_INTERNAL_ERROR")
        fun mapReasonCode_unknown() {
            assertThat(adapter.mapReasonCode("알 수 없는 에러"))
                .isEqualTo(PaymentReasonCode.PG_INTERNAL_ERROR)
        }
    }
}
