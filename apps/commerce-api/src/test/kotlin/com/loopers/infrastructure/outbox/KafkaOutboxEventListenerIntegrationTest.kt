package com.loopers.infrastructure.outbox

import com.loopers.utils.DatabaseCleanUp
import com.loopers.support.event.user.ProductDetailViewedEvent
import com.loopers.support.event.user.CouponIssueRequestedEvent
import com.loopers.support.event.user.ProductLikeRegisteredEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@DisplayName("Kafka outbox event listener integration")
@SpringBootTest
class KafkaOutboxEventListenerIntegrationTest
@Autowired
constructor(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val kafkaOutboxJpaRepository: KafkaOutboxJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    platformTransactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(platformTransactionManager)

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("transaction commit 후 outbox row가 남는다")
    fun persist_afterCommit() {
        transactionTemplate.executeWithoutResult {
            applicationEventPublisher.publishEvent(
                ProductLikeRegisteredEvent(userId = USER_ID, productId = PRODUCT_ID),
            )
        }

        val rows = kafkaOutboxJpaRepository.findAll()
        assertThat(rows).hasSize(1)
        assertThat(rows[0].topic).isEqualTo("catalog-events")
        assertThat(rows[0].publishedAt).isNull()
    }

    @Test
    @DisplayName("transaction rollback 시 outbox row가 남지 않는다")
    fun persist_notWritten_onRollback() {
        runCatching {
            transactionTemplate.executeWithoutResult {
                applicationEventPublisher.publishEvent(
                    ProductLikeRegisteredEvent(userId = USER_ID, productId = PRODUCT_ID),
                )
                error("rollback")
            }
        }

        assertThat(kafkaOutboxJpaRepository.findAll()).isEmpty()
    }

    @Test
    @DisplayName("상품 상세 조회 이벤트도 transaction rollback 시 outbox row가 남지 않는다")
    fun persist_detailViewed_notWritten_onRollback() {
        runCatching {
            transactionTemplate.executeWithoutResult {
                applicationEventPublisher.publishEvent(
                    ProductDetailViewedEvent(productId = PRODUCT_ID),
                )
                error("rollback")
            }
        }

        assertThat(kafkaOutboxJpaRepository.findAll()).isEmpty()
    }

    @Test
    @DisplayName("coupon issue 요청 이벤트도 transaction rollback 시 outbox row가 남지 않는다")
    fun persist_couponIssueRequested_notWritten_onRollback() {
        runCatching {
            transactionTemplate.executeWithoutResult {
                applicationEventPublisher.publishEvent(
                    CouponIssueRequestedEvent(
                        requestId = 100L,
                        couponId = 200L,
                        userId = USER_ID,
                    ),
                )
                error("rollback")
            }
        }

        assertThat(kafkaOutboxJpaRepository.findAll()).isEmpty()
    }

    companion object {
        private const val PRODUCT_ID = 100L
        private const val USER_ID = 10L
    }
}
