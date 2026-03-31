package com.loopers.event

object EventContract {
    // --- topics ---
    const val PAYMENT_SUCCEEDED_TOPIC = "payment.succeeded"
    const val PAYMENT_FAILED_TOPIC = "payment.failed"
    const val PRODUCT_ACTION_TOPIC = "product.action"
    const val COUPON_ISSUE_REQUEST_TOPIC = "coupon.issue.request"

    // --- aggregate types ---
    const val AGGREGATE_ORDER = "ORDER"
    const val AGGREGATE_PRODUCT = "PRODUCT"
    const val AGGREGATE_FCFS_COUPON = "FCFS_COUPON"

    // --- event types ---
    const val EVENT_PAYMENT_SUCCEEDED = "PaymentSucceeded"
    const val EVENT_PAYMENT_FAILED = "PaymentFailed"
    const val EVENT_FCFS_COUPON_ISSUE_REQUESTED = "FcfsCouponIssueRequested"
}
