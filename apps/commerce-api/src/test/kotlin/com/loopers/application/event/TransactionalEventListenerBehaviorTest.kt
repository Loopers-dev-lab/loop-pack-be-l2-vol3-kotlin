package com.loopers.application.event

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.atomic.AtomicInteger

@SpringJUnitConfig(TransactionalEventListenerBehaviorTest.TestConfig::class)
@DisplayName("TransactionalEventListener AFTER_COMMIT 동작")
class TransactionalEventListenerBehaviorTest {
    @Configuration
    @EnableTransactionManagement
    class TestConfig {
        @Bean
        fun transactionManager(): PlatformTransactionManager = NoOpTransactionManager()

        @Bean
        fun transactionTemplate(transactionManager: PlatformTransactionManager): TransactionTemplate {
            return TransactionTemplate(transactionManager)
        }

        @Bean
        fun userActionLogWriter(): RecordingUserActionLogWriter = RecordingUserActionLogWriter()

        @Bean
        fun userActionLoggingEventListener(
            userActionLogWriter: UserActionLogWriter,
        ): UserActionLoggingEventListener = UserActionLoggingEventListener(userActionLogWriter)
    }

    class NoOpTransactionManager : AbstractPlatformTransactionManager() {
        override fun doGetTransaction(): Any = Any()

        override fun doBegin(transaction: Any, definition: TransactionDefinition) {
        }

        override fun doCommit(status: DefaultTransactionStatus) {
        }

        override fun doRollback(status: DefaultTransactionStatus) {
        }
    }

    class RecordingUserActionLogWriter : UserActionLogWriter {
        val writeAttempts = AtomicInteger(0)
        val writtenCount = AtomicInteger(0)
        var shouldFail: Boolean = false

        override fun write(command: UserActionLogCommand) {
            writeAttempts.incrementAndGet()
            if (shouldFail) {
                throw IllegalStateException("write failure")
            }
            writtenCount.incrementAndGet()
        }

        fun clear() {
            writeAttempts.set(0)
            writtenCount.set(0)
            shouldFail = false
        }
    }

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var userActionLogWriter: RecordingUserActionLogWriter

    @BeforeEach
    fun setUp() {
        userActionLogWriter.clear()
    }

    @DisplayName("리스너에서 예외가 발생해도 주문 명령 성공은 실패로 바뀌지 않는다")
    @Test
    fun orderSuccessIsNotCoupledToListenerFailure() {
        userActionLogWriter.shouldFail = true

        assertThatCode {
            transactionTemplate.executeWithoutResult {
                applicationEventPublisher.publishEvent(
                    OrderCompletedEvent(
                        orderId = 100L,
                        userId = 1L,
                        totalAmount = 30_000L,
                    ),
                )
            }
        }.doesNotThrowAnyException()

        assertThat(userActionLogWriter.writeAttempts.get()).isEqualTo(1)
        assertThat(userActionLogWriter.writtenCount.get()).isEqualTo(0)
    }

    @DisplayName("트랜잭션 롤백 시 AFTER_COMMIT 리스너가 실행되지 않는다")
    @Test
    fun afterCommitListenerDoesNotRunOnRollback() {
        assertThatThrownBy {
            transactionTemplate.executeWithoutResult {
                applicationEventPublisher.publishEvent(
                    OrderCompletedEvent(
                        orderId = 100L,
                        userId = 1L,
                        totalAmount = 30_000L,
                    ),
                )
                throw IllegalStateException("force rollback")
            }
        }.isInstanceOf(IllegalStateException::class.java)

        assertThat(userActionLogWriter.writeAttempts.get()).isEqualTo(0)
        assertThat(userActionLogWriter.writtenCount.get()).isEqualTo(0)
    }
}
