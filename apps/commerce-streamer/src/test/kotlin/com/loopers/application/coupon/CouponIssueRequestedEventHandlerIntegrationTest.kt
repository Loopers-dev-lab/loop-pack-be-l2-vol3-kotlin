package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.infrastructure.outbox.CouponIssueRequestedOutboxMessagePayload
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("CouponIssueRequestedEventHandler integration")
@SpringBootTest(classes = [CouponIssueRequestedEventHandlerIntegrationTest.TestApplication::class])
@ActiveProfiles("test")
@TestPropertySource(
    properties = [
        "datasource.mysql-jpa.main.jdbc-url=jdbc:mysql://localhost:3306/loopers",
        "datasource.mysql-jpa.main.driver-class-name=com.mysql.cj.jdbc.Driver",
        "datasource.mysql-jpa.main.username=application",
        "datasource.mysql-jpa.main.password=application",
        "datasource.redis.master.host=localhost",
        "datasource.redis.master.port=6379",
        "datasource.redis.replicas[0].host=localhost",
        "datasource.redis.replicas[0].port=6380",
    ],
)
class CouponIssueRequestedEventHandlerIntegrationTest
@Autowired
constructor(
    private val handler: CouponIssueRequestedEventHandler,
    private val couponRepository: CouponRepository,
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("동일 쿠폰에 대한 두 발급 요청을 동시에 처리해도 1건만 발급되고 나머지는 SOLD_OUT으로 마감된다")
    fun handle_concurrentRequests_onlyOneIssued() {
        val coupon = couponRepository.save(
            Coupon.register(
                name = "선착순 쿠폰",
                type = Coupon.Type.FIXED,
                discountValue = 1000L,
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
                issueLimit = 1L,
            ),
        )
        val firstRequest = couponIssueRequestRepository.save(
            CouponIssueRequest.request(
                couponId = coupon.id!!,
                userId = 1L,
            ),
        )
        val secondRequest = couponIssueRequestRepository.save(
            CouponIssueRequest.request(
                couponId = coupon.id!!,
                userId = 2L,
            ),
        )

        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(2)
        val failures = ConcurrentLinkedQueue<Throwable>()
        val executor = Executors.newFixedThreadPool(2)

        listOf(
            CouponIssueRequestedOutboxMessagePayload(firstRequest.id!!, coupon.id!!, 1L),
            CouponIssueRequestedOutboxMessagePayload(secondRequest.id!!, coupon.id!!, 2L),
        ).forEach { payload ->
            executor.submit {
                try {
                    startLatch.await()
                    handler.handle(payload)
                } catch (throwable: Throwable) {
                    failures.add(throwable)
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        val completed = doneLatch.await(5, TimeUnit.SECONDS)
        executor.shutdown()

        assertThat(completed).isTrue()
        assertThat(failures).isEmpty()

        val updatedFirst = couponIssueRequestRepository.findById(firstRequest.id!!)!!
        val updatedSecond = couponIssueRequestRepository.findById(secondRequest.id!!)!!
        val savedCoupon = couponRepository.findById(coupon.id!!)!!

        assertThat(listOf(updatedFirst.status, updatedSecond.status))
            .containsExactlyInAnyOrder(
                CouponIssueRequest.Status.ISSUED,
                CouponIssueRequest.Status.FAILED,
            )
        assertThat(listOf(updatedFirst.failureReasonCode, updatedSecond.failureReasonCode))
            .containsExactlyInAnyOrder(null, "COUPON_SOLD_OUT")
        assertThat(listOf(updatedFirst.issuedCouponId, updatedSecond.issuedCouponId).count { it != null })
            .isEqualTo(1)
        assertThat(savedCoupon.issuedCount).isEqualTo(1L)
        assertThat(
            listOfNotNull(
                issuedCouponRepository.findByCouponIdAndUserId(coupon.id!!, 1L),
                issuedCouponRepository.findByCouponIdAndUserId(coupon.id!!, 2L),
            ),
        ).hasSize(1)
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ConfigurationPropertiesScan("com.loopers")
    @ComponentScan(
        basePackages = [
            "com.loopers.application",
            "com.loopers.domain",
            "com.loopers.config",
            "com.loopers.infrastructure",
            "com.loopers.interfaces",
            "com.loopers.support",
            "com.loopers.utils",
        ],
    )
    class TestApplication
}
