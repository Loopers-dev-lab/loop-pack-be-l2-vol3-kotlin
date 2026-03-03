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
import org.junit.jupiter.api.Timeout

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LikeConcurrencyTest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

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
        testRestTemplate.exchange(
            "/api/v1/users/sign-up",
            HttpMethod.POST,
            HttpEntity(request),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
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
        val brandResponseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
        val brandResponse = testRestTemplate.exchange(
            "/api-admin/v1/brands?name={name}",
            HttpMethod.POST,
            HttpEntity<Void>(adminHeaders()),
            brandResponseType,
            "나이키",
        )
        val brandId = (brandResponse.body!!.data!!["id"] as Number).toLong()

        val productRequest = ProductAdminV1Dto.CreateProductRequest(
            brandId = brandId,
            name = "에어맥스 90",
            price = BigDecimal("129000"),
            stock = 100,
        )
        val productResponseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
        val productResponse = testRestTemplate.exchange(
            "/api-admin/v1/products",
            HttpMethod.POST,
            HttpEntity(productRequest, adminHeaders()),
            productResponseType,
        )
        return (productResponse.body!!.data!!["id"] as Number).toLong()
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

            // act
            for (i in 1..concurrentUsers) {
                executorService.submit {
                    try {
                        readyLatch.countDown()
                        startLatch.await(30, TimeUnit.SECONDS)
                        val responseType =
                            object : ParameterizedTypeReference<ApiResponse<Any>>() {}
                        val response = testRestTemplate.exchange(
                            "/api/v1/products/$productId/likes",
                            HttpMethod.POST,
                            HttpEntity<Any>(authHeaders("likeuser$i")),
                            responseType,
                        )
                        if (response.statusCode == HttpStatus.OK) {
                            successCount.incrementAndGet()
                        } else {
                            failCount.incrementAndGet()
                        }
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            readyLatch.await(30, TimeUnit.SECONDS)
            startLatch.countDown()
            latch.await(30, TimeUnit.SECONDS)
            executorService.shutdown()

            // assert - 어드민 API로 likeCount 확인
            val adminProductType =
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val adminResponse = testRestTemplate.exchange(
                "/api-admin/v1/products/$productId",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                adminProductType,
            )
            val likeCount =
                (adminResponse.body!!.data!!["likeCount"] as Number).toInt()

            assertAll(
                { assertThat(successCount.get()).isEqualTo(concurrentUsers) },
                { assertThat(likeCount).isEqualTo(concurrentUsers) },
            )
        }
    }
}
