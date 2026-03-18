package com.loopers.support.transaction

import org.slf4j.LoggerFactory
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
                    runCatching { action() }
                        .onFailure { exception ->
                            log.warn("After-commit action failed", exception)
                        }
                }
            },
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(TransactionSynchronizationAfterCommitExecutor::class.java)
    }
}
