package com.loopers.interfaces.api.like

import com.loopers.interfaces.api.product.dto.ProductAdminV1Dto
import com.loopers.interfaces.api.user.dto.UserV1Dto
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.HEADER_LDAP
import com.loopers.interfaces.support.HEADER_LOGIN_ID
import com.loopers.interfaces.support.HEADER_LOGIN_PW
import com.loopers.interfaces.support.LDAP_ADMIN_VALUE
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.jupiter.api.Timeout

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LikeConcurrencyTest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    companion object {
        val ANY_TYPE = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
        val MAP_TYPE = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun signUp(loginId: String) {
        val request = UserV1Dto.SignUpRequest(
            loginId = loginId,
            password = "Password1!",
            name = "테스트유저",
            birthDate = LocalDate.of(1990, 1, 15),
            email = "$loginId@example.com",
        )
        val response = testRestTemplate.exchange(
            "/api/v1/users/sign-up",
            HttpMethod.POST,
            HttpEntity(request),
            ANY_TYPE,
        )
        assertThat(response.statusCode.is2xxSuccessful)
            .describedAs("회원가입 API 호출이 성공해야 합니다: ${response.statusCode}")
            .isTrue()
    }

    private fun authHeaders(loginId: String): HttpHeaders {
        return HttpHeaders().apply {
            set(HEADER_LOGIN_ID, loginId)
            set(HEADER_LOGIN_PW, "Password1!")
            set("Content-Type", "application/json")
        }
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(HEADER_LDAP, LDAP_ADMIN_VALUE)
            set("Content-Type", "application/json")
        }
    }

    private fun createBrandAndProduct(): Long {
        val brandResponse = testRestTemplate.exchange(
            "/api-admin/v1/brands?name={name}",
            HttpMethod.POST,
            HttpEntity<Void>(adminHeaders()),
            MAP_TYPE,
            "나이키",
        )
        assertThat(brandResponse.statusCode.is2xxSuccessful)
            .describedAs("브랜드 생성 API 호출이 성공해야 합니다: ${brandResponse.statusCode}")
            .isTrue()
        val brandBody = requireNotNull(brandResponse.body) { "브랜드 생성 응답 body가 null입니다" }
        val brandData = requireNotNull(brandBody.data) { "브랜드 생성 응답 data가 null입니다" }
        val brandId = (brandData["id"] as Number).toLong()

        val productRequest = ProductAdminV1Dto.CreateProductRequest(
            brandId = brandId,
            name = "에어맥스 90",
            price = BigDecimal("129000"),
            stock = 100,
        )
        val productResponse = testRestTemplate.exchange(
            "/api-admin/v1/products",
            HttpMethod.POST,
            HttpEntity(productRequest, adminHeaders()),
            MAP_TYPE,
        )
        assertThat(productResponse.statusCode.is2xxSuccessful)
            .describedAs("상품 생성 API 호출이 성공해야 합니다: ${productResponse.statusCode}")
            .isTrue()
        val productBody = requireNotNull(productResponse.body) { "상품 생성 응답 body가 null입니다" }
        val productData = requireNotNull(productBody.data) { "상품 생성 응답 data가 null입니다" }
        return (productData["id"] as Number).toLong()
    }

    @Nested
    @DisplayName("좋아요 동시성 테스트")
    inner class ConcurrentLike {

        @Test
        @Timeout(60)
        @DisplayName("N명이 동시에 같은 상품에 좋아요하면 likeCount가 N이 된다")
        fun concurrentLike_likeCountEqualsN() {
            // arrange
            val concurrentUsers = 20
            val productId = createBrandAndProduct()

            for (i in 1..concurrentUsers) {
                signUp("likeuser$i")
            }

            val executorService = Executors.newFixedThreadPool(concurrentUsers)
            val readyLatch = CountDownLatch(concurrentUsers)
            val startLatch = CountDownLatch(1)
            val latch = CountDownLatch(concurrentUsers)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)
            val firstException = AtomicReference<Exception>()

            // act
            try {
                for (i in 1..concurrentUsers) {
                    executorService.submit {
                        try {
                            readyLatch.countDown()
                            val started = startLatch.await(30, TimeUnit.SECONDS)
                            if (!started) {
                                failCount.incrementAndGet()
                                return@submit
                            }
                            val response = testRestTemplate.exchange(
                                "/api/v1/products/$productId/likes",
                                HttpMethod.POST,
                                HttpEntity<Any>(authHeaders("likeuser$i")),
                                ANY_TYPE,
                            )
                            if (response.statusCode == HttpStatus.OK) {
                                successCount.incrementAndGet()
                            } else {
                                failCount.incrementAndGet()
                            }
                        } catch (e: Exception) {
                            failCount.incrementAndGet()
                            firstException.compareAndSet(null, e)
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                val allReady = readyLatch.await(30, TimeUnit.SECONDS)
                assertThat(allReady).describedAs("모든 스레드가 준비되어야 합니다").isTrue()
                startLatch.countDown()
                val allDone = latch.await(30, TimeUnit.SECONDS)
                assertThat(allDone).describedAs("모든 스레드가 완료되어야 합니다").isTrue()
            } finally {
                executorService.shutdownNow()
                val terminated = executorService.awaitTermination(5, TimeUnit.SECONDS)
                assertThat(terminated).describedAs("스레드풀이 제한 시간 내에 종료되어야 합니다").isTrue()
            }

            // assert - 어드민 API로 likeCount 확인
            val adminResponse = testRestTemplate.exchange(
                "/api-admin/v1/products/$productId",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                MAP_TYPE,
            )
            val adminBody = requireNotNull(adminResponse.body) { "좋아요 조회 응답 body가 null입니다" }
            val adminData = requireNotNull(adminBody.data) { "좋아요 조회 응답 data가 null입니다" }
            val likeCount = (adminData["likeCount"] as Number).toInt()

            assertAll(
                { assertThat(successCount.get()).isEqualTo(concurrentUsers) },
                { assertThat(failCount.get()).describedAs("실패 건수").isEqualTo(0) },
                { assertThat(firstException.get()).describedAs("첫 번째 예외").isNull() },
                { assertThat(likeCount).isEqualTo(concurrentUsers) },
            )
        }
    }
}
