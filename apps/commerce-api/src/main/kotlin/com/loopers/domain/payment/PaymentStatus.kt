package com.loopers.domain.payment

enum class PaymentStatus {
    REQUESTED,
    PENDING,
    SUCCESS,
    FAILED,
    ;

    fun canTransitionTo(next: PaymentStatus): Boolean {
        return when (this) {
            REQUESTED -> next == PENDING || next == FAILED
            PENDING -> next == SUCCESS || next == FAILED
            SUCCESS -> false
            FAILED -> false
        }
    }
}
