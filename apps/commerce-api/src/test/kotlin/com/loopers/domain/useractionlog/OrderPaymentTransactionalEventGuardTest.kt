package com.loopers.domain.useractionlog

import com.loopers.CommerceApiApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest(classes = [CommerceApiApplication::class])
@Import(OrderPaymentTransactionalEventGuardTest.TestConfig::class)
@DisplayName("Order/Payment 트랜잭셔널 이벤트 가드 테스트")
class OrderPaymentTransactionalEventGuardTest @Autowired constructor(
    private val eventPublisher: ApplicationEventPublisher,
    transactionManager: PlatformTransactionManager,
    private val probeStore: ProbeStore,
) {

    private val transactionTemplate = TransactionTemplate(transactionManager)

    @BeforeEach
    fun setUp() {
        probeStore.clear()
    }

    @Test
    @DisplayName("트랜잭션 없이 publish 하면 AFTER_COMMIT 리스너가 부수효과를 저장하지 않는다")
    fun doesNotPersistWhenPublishedWithoutTransaction() {
        // when
        eventPublisher.publishEvent(ProbeEvent("without-transaction"))

        // then
        assertThat(probeStore.persistedPayloads()).isEmpty()
    }

    @Test
    @DisplayName("트랜잭션 커밋 시 AFTER_COMMIT 리스너가 부수효과를 저장한다")
    fun persistsWhenPublishedWithinCommittedTransaction() {
        // when
        transactionTemplate.executeWithoutResult {
            eventPublisher.publishEvent(ProbeEvent("committed"))
        }

        // then
        assertThat(probeStore.persistedPayloads()).containsExactly("committed")
    }

    @Test
    @DisplayName("트랜잭션 롤백 시 AFTER_COMMIT 리스너가 부수효과를 저장하지 않는다")
    fun doesNotPersistWhenTransactionRollsBack() {
        // when
        transactionTemplate.executeWithoutResult { status ->
            eventPublisher.publishEvent(ProbeEvent("rolled-back"))
            status.setRollbackOnly()
        }

        // then
        assertThat(probeStore.persistedPayloads()).isEmpty()
    }

    @Test
    @DisplayName("OrderPaymentUserActionLogEventListener는 트랜잭션 커밋 경계 설정을 유지한다")
    fun orderPaymentUserActionLogEventListenerMethodsStayTransactionBound() {
        val methods = OrderPaymentUserActionLogEventListener::class.java.declaredMethods
            .filter {
                it.name == "onOrderCreated" ||
                    it.name == "onPaymentRequested" ||
                    it.name == "onPaymentCallbackProcessed"
            }

        assertThat(methods.map { it.name }).containsExactlyInAnyOrder(
            "onOrderCreated",
            "onPaymentRequested",
            "onPaymentCallbackProcessed",
        )

        methods.forEach { method ->
            val annotation = method.getAnnotation(TransactionalEventListener::class.java)
            assertThat(annotation).describedAs("%s must use @TransactionalEventListener", method.name).isNotNull
            assertThat(annotation.phase)
                .describedAs("%s must stay AFTER_COMMIT", method.name)
                .isEqualTo(TransactionPhase.AFTER_COMMIT)
            assertThat(annotation.fallbackExecution)
                .describedAs("%s must not run without transaction", method.name)
                .isFalse()
        }
    }

    data class ProbeEvent(val payload: String)

    class ProbeStore {
        private val payloads = mutableListOf<String>()

        fun persist(payload: String) {
            payloads += payload
        }

        fun persistedPayloads(): List<String> = payloads.toList()

        fun clear() {
            payloads.clear()
        }
    }

    class ProbeEventListener(
        private val probeStore: ProbeStore,
    ) {
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
        fun onProbeEvent(event: ProbeEvent) {
            probeStore.persist(event.payload)
        }
    }

    @Configuration
    class TestConfig {
        @Bean
        fun probeStore(): ProbeStore = ProbeStore()

        @Bean
        fun probeEventListener(probeStore: ProbeStore): ProbeEventListener = ProbeEventListener(probeStore)
    }
}
