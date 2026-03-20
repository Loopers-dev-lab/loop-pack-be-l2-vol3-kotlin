package com.loopers.application.order

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderTransactionRunner {
    @Transactional
    fun <T> runInTransaction(block: () -> T): T {
        return block()
    }
}
