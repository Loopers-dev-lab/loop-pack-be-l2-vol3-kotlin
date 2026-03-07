package com.loopers.concurrency

import com.loopers.application.order.OrderFacade
import com.loopers.application.order.OrderItemCriteria
import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 비관적 락의 구조적 한계를 증명하는 테스트.
 *
 * [테스트 환경]
 * - 커넥션 풀: 5개 (@TestPropertySource로 오버라이드)
 * - connection-timeout: 250ms (@TestPropertySource로 오버라이드, HikariCP 최솟값)
 * - 비관적 락(SELECT ... FOR UPDATE)은 트랜잭션 종료까지 커넥션을 점유
 *
 * [시나리오]
 * - 재고 100개인 상품에 100개 스레드가 동시에 1개씩 주문
 * - 비관적 락이 트랜잭션 종료까지 커넥션을 점유하므로,
 *   5개 커넥션이 모두 락 대기 중이면 나머지 95개 스레드는 커넥션 자체를 획득 못 함
 * - 250ms 내에 커넥션을 못 얻은 스레드는 SQLTransientConnectionException으로 실패
 *
 * [기대 결과]
 * - 일부 스레드가 커넥션 풀 고갈로 실패 (failCount > 0)
 * - 재고가 남아있음에도 실패 (= 비즈니스 로직이 아닌 인프라 한계)
 * - 이는 비관적 락이 "정합성은 보장하지만 처리량에 한계가 있다"는 것을 증명
 *
 * [핵심 포인트]
 * 비관적 락의 문제는 "락 자체"가 아니라 "락을 기다리는 동안 커넥션을 붙잡고 있는 것"이다.
 * 같은 재고 차감이라도 Atomic Update(UPDATE ... SET stock = stock - 1 WHERE stock >= 1)를 쓰면
 * 락 보유 시간 ≈ 0이므로 커넥션이 즉시 반환되어 이 문제가 발생하지 않는다.
 */
@DisplayName("비관적 락 커넥션 풀 고갈 테스트")
@SpringBootTest
@TestPropertySource(
    properties = [
        "datasource.mysql-jpa.main.maximum-pool-size=5",
        "datasource.mysql-jpa.main.minimum-idle=5",
        "datasource.mysql-jpa.main.connection-timeout=250",
    ],
)
class ConnectionPoolExhaustionTest @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("커넥션 풀(5개)보다 훨씬 많은 스레드(100개)가 비관적 락을 사용하면, 커넥션 풀 고갈로 일부 주문이 실패한다.")
    @Test
    fun connectionPoolExhaustion_whenConcurrentPessimisticLockOrders() {
        // arrange
        val threadCount = 100
        val initialStock = 100
        val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        val product = productJpaRepository.save(
            Product(
                brandId = brand.id,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = initialStock,
                description = null,
                imageUrl = null,
            ),
        )

        val latch = CountDownLatch(1)
        val executorService = Executors.newFixedThreadPool(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // act
        repeat(threadCount) { index ->
            executorService.submit {
                try {
                    latch.await()
                    val criteria = listOf(OrderItemCriteria(productId = product.id, quantity = 1))
                    orderFacade.createOrder(userId = index.toLong() + 1, criteria = criteria)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                }
            }
        }

        latch.countDown()
        executorService.shutdown()
        executorService.awaitTermination(30, TimeUnit.SECONDS)

        // assert
        val updatedProduct = productJpaRepository.findById(product.id).get()

        assertAll(
            // 커넥션 풀 고갈로 일부 스레드가 실패해야 한다
            { assertThat(failCount.get()).isGreaterThan(0) },
            // 재고가 남아있다 = 비즈니스 로직(재고 부족)이 아닌 인프라 한계로 실패
            { assertThat(updatedProduct.stock).isGreaterThan(0) },
            // 성공 + 실패 = 전체 스레드 수
            { assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount) },
        )
    }
}
