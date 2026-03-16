package com.loopers.support.transaction

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.atomic.AtomicBoolean

@SpringBootTest
@DisplayName("AfterCommitExecutor")
class AfterCommitExecutorTest
@Autowired
constructor(
    private val afterCommitExecutor: AfterCommitExecutor,
    platformTransactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(platformTransactionManager)

    @Test
    @DisplayName("트랜잭션이 commit 되면 afterCommit 에 등록한 작업이 실행된다")
    fun execute_afterCommit() {
        val executed = AtomicBoolean(false)

        transactionTemplate.executeWithoutResult {
            afterCommitExecutor.execute { executed.set(true) }

            assertThat(executed.get()).isFalse()
        }

        assertThat(executed.get()).isTrue()
    }

    @Test
    @DisplayName("트랜잭션이 rollback 되면 afterCommit 에 등록한 작업은 실행되지 않는다")
    fun execute_afterRollback() {
        val executed = AtomicBoolean(false)

        transactionTemplate.executeWithoutResult { status ->
            afterCommitExecutor.execute { executed.set(true) }

            status.setRollbackOnly()
            assertThat(executed.get()).isFalse()
        }

        assertThat(executed.get()).isFalse()
    }

    @Test
    @DisplayName("활성 트랜잭션이 없으면 작업을 즉시 실행한다")
    fun execute_withoutTransaction() {
        val executed = AtomicBoolean(false)

        afterCommitExecutor.execute { executed.set(true) }

        assertThat(executed.get()).isTrue()
    }
}
