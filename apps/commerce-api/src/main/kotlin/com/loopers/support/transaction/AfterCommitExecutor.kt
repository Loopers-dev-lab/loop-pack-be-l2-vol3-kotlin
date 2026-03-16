package com.loopers.support.transaction

import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

fun interface AfterCommitExecutor {
    fun execute(action: () -> Unit)
}

@Component
class TransactionSynchronizationAfterCommitExecutor : AfterCommitExecutor {
    override fun execute(action: () -> Unit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action()
            return
        }

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    action()
                }
            },
        )
    }
}
