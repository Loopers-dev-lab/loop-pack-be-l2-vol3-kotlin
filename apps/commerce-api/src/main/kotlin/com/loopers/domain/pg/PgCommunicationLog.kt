package com.loopers.domain.pg

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "pg_communication_logs",
    indexes = [
        Index(name = "idx_pg_log_transaction_key", columnList = "transaction_key"),
        Index(name = "idx_pg_log_order_id", columnList = "order_id"),
        Index(name = "idx_pg_log_created_at", columnList = "created_at"),
    ],
)
class PgCommunicationLog(
    method: String,
    url: String,
    orderId: String?,
    transactionKey: String?,
    requestBody: String?,
    responseBody: String?,
    httpStatus: Int?,
    success: Boolean,
    errorMessage: String?,
    elapsed: Long,
) : BaseEntity() {

    @Column(name = "method", nullable = false, length = 10)
    val method: String = method

    @Column(name = "url", nullable = false, length = 200)
    val url: String = url

    @Column(name = "order_id", length = 50)
    val orderId: String? = orderId

    @Column(name = "transaction_key", length = 50)
    val transactionKey: String? = transactionKey

    @Column(name = "request_body", columnDefinition = "TEXT")
    val requestBody: String? = requestBody

    @Column(name = "response_body", columnDefinition = "TEXT")
    val responseBody: String? = responseBody

    @Column(name = "http_status")
    val httpStatus: Int? = httpStatus

    @Column(name = "success", nullable = false)
    val success: Boolean = success

    @Column(name = "error_message", length = 500)
    val errorMessage: String? = errorMessage

    @Column(name = "elapsed", nullable = false)
    val elapsed: Long = elapsed
}
