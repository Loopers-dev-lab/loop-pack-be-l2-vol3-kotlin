package com.loopers.interfaces.api.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.stock.Stock
import com.loopers.domain.user.User
import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.stock.StockJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.math.BigDecimal
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiConcurrencyE2ETest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val stockJpaRepository: StockJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val passwordEncoder: PasswordEncoder,
) {

    companion object {
        private const val ENDPOINT_ORDERS = "/api/v1/orders"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createAuthHeaders(loginId: String, password: String): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", loginId)
            set("X-Loopers-LoginPw", password)
        }
    }

    @Test
    @DisplayName("동시에 주문 생성 시 재고가 올바르게 감소한다 (10개 스레드, 재고 10개)")
    fun testConcurrentOrderCreation() {
        // Arrange
        val plainPassword = "password123"
        val threadCount = 10

        // 10명의 사용자 생성
        val users = (1..threadCount).map { i ->
            val user = User.create(
                loginId = LoginId.of("user$i"),
                password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
                name = Name.of("사용자$i"),
                birthDate = BirthDate.of("20260101"),
                email = Email.of("user$i@test.com"),
            )
            userJpaRepository.save(user)
        }

        // 상품 및 재고 생성 (재고 10개)
        val brand = Brand.create(
            name = "동시성 테스트 브랜드",
            description = "동시성 테스트 브랜드",
        )
        val savedBrand = brandJpaRepository.save(brand)

        val product = Product.create(
            brand = savedBrand,
            name = "동시성 테스트 상품",
            price = BigDecimal("10000"),
            status = ProductStatus.ACTIVE,
        )
        val savedProduct = productJpaRepository.save(product)

        stockJpaRepository.save(
            Stock.create(
                productId = savedProduct.id,
                quantity = 10,
            ),
        )

        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        val results = Collections.synchronizedList(mutableListOf<OrderResult>())

        // Act: 10개 스레드가 동시에 주문 생성
        val tasks = users.mapIndexed { index, user ->
            executor.submit {
                latch.countDown()
                latch.await()

                try {
                    val request = OrderV1Dto.OrderRequest(
                        items = listOf(
                            OrderV1Dto.OrderItemRequest(
                                productId = savedProduct.id,
                                quantity = 2,
                            ),
                        ),
                    )

                    val headers = createAuthHeaders("user${index + 1}", plainPassword)
                    val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
                    val response = restTemplate.exchange(
                        ENDPOINT_ORDERS,
                        HttpMethod.POST,
                        HttpEntity(request, headers),
                        responseType,
                    )

                    if (response.statusCode == HttpStatus.CREATED) {
                        results.add(OrderResult.Success(response.body?.data ?: 0L))
                    } else {
                        results.add(OrderResult.Failure(response.statusCode.toString()))
                    }
                } catch (e: Exception) {
                    results.add(OrderResult.Failure(e.javaClass.simpleName))
                }
            }
        }

        tasks.forEach { task ->
            try {
                task.get(10, TimeUnit.SECONDS)
            } catch (e: java.util.concurrent.TimeoutException) {
                throw AssertionError("Task timeout after 10 seconds")
            }
        }
        executor.shutdown()
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            executor.shutdownNow()
            throw AssertionError("Executor did not terminate within 10 seconds")
        }

        // Assert
        val successCount = results.count { it is OrderResult.Success }

        // 재고 10개, 각 2개씩 → 최대 5개 성공
        assertThat(successCount).isLessThanOrEqualTo(5)
        assertThat(successCount).isGreaterThan(0) // 최소 1개는 성공해야 함

        // 최종 재고 확인
        val finalStock = stockJpaRepository.findByProductId(savedProduct.id)
        assertThat(finalStock?.quantity).isEqualTo(10 - (successCount * 2))
    }

    sealed class OrderResult {
        data class Success(val orderId: Long) : OrderResult()
        data class Failure(val reason: String) : OrderResult()
    }
}
