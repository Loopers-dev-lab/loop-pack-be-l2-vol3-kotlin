package com.loopers.application.coupon

import com.loopers.infrastructure.coupon.CouponIssueRequestEntity
import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponEntity
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class CouponIssueRequestProcessorConcurrencyTest {

    @Test
    fun `동시에_여러_요청이_와도_수량만큼만_성공한다`() {
        val requestRepository = mockk<CouponIssueRequestJpaRepository>()
        val couponRepository = mockk<CouponJpaRepository>()
        val issuedCouponRepository = mockk<IssuedCouponJpaRepository>()
        val processor = CouponIssueRequestProcessor(
            couponIssueRequestJpaRepository = requestRepository,
            couponJpaRepository = couponRepository,
            issuedCouponJpaRepository = issuedCouponRepository,
        )

        val limit = 10
        val issuedCount = AtomicInteger(0)
        val requests = ConcurrentHashMap<Long, CouponIssueRequestEntity>()
        val issuedPairs = ConcurrentHashMap.newKeySet<String>()
        val sequence = AtomicInteger(1000)

        repeat(30) { index ->
            val requestId = (index + 1).toLong()
            requests[requestId] = CouponIssueRequestEntity(
                id = requestId,
                couponId = 1L,
                memberId = requestId,
                status = "PENDING",
                requestedAt = ZonedDateTime.now(),
            )
        }

        every { requestRepository.findById(any()) } answers {
            Optional.ofNullable(requests[firstArg<Long>()])
        }
        every { requestRepository.save(any()) } answers {
            val entity = firstArg<CouponIssueRequestEntity>()
            requests[requireNotNull(entity.id)] = entity
            entity
        }
        every { couponRepository.tryIncreaseIssuedCount(1L) } answers {
            synchronized(issuedCount) {
                if (issuedCount.get() >= limit) {
                    0
                } else {
                    issuedCount.incrementAndGet()
                    1
                }
            }
        }
        every { issuedCouponRepository.existsByCouponIdAndMemberId(any(), any()) } answers {
            val couponId = firstArg<Long>()
            val memberId = secondArg<Long>()
            issuedPairs.contains("$couponId:$memberId")
        }
        every { issuedCouponRepository.save(any()) } answers {
            val entity = firstArg<IssuedCouponEntity>()
            issuedPairs.add("${entity.couponId}:${entity.memberId}")
            IssuedCouponEntity(
                id = sequence.incrementAndGet().toLong(),
                couponId = entity.couponId,
                memberId = entity.memberId,
                status = entity.status,
                issuedAt = entity.issuedAt,
            )
        }

        val executor = Executors.newFixedThreadPool(8)
        val latch = CountDownLatch(requests.size)
        requests.keys.forEach { requestId ->
            executor.submit {
                try {
                    processor.process(
                        requestId = requestId,
                        couponId = 1L,
                        memberId = requestId,
                    )
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executor.shutdown()

        val succeeded = requests.values.count { it.status == "SUCCEEDED" }
        val soldOut = requests.values.count { it.status == "FAILED_SOLD_OUT" }

        assertThat(succeeded).isEqualTo(limit)
        assertThat(soldOut).isEqualTo(requests.size - limit)
        assertThat(issuedPairs).hasSize(limit)
    }
}
