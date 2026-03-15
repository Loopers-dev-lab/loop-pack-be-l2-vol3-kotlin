package com.loopers.interfaces.api.v1.coupon

import com.loopers.interfaces.api.auth.AuthenticationFilter
import com.loopers.testcontainers.MySqlTestContainersConfig
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.jdbc.Sql
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestContainersConfig::class)
@Sql(
    statements = [
        "DELETE FROM order_item",
        "DELETE FROM orders",
        "DELETE FROM user_coupon",
        "DELETE FROM coupon",
        "DELETE FROM likes",
        "DELETE FROM product_image",
        "DELETE FROM product",
        "DELETE FROM brand",
        "DELETE FROM users",
    ],
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class CouponApiE2ETest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
    }

    @Nested
    inner class IssueCoupon {
        @Test
        fun `쿠폰을 발급받을 수 있다`() {
            registerUser()
            val couponId = createCoupon()

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .post("/api/v1/coupons/$couponId/issue")
            .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.id", notNullValue())
        }

        @Test
        fun `이미 발급받은 쿠폰을 재발급하면 409 에러가 발생한다`() {
            registerUser()
            val couponId = createCoupon()
            issueCoupon(couponId)

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .post("/api/v1/coupons/$couponId/issue")
            .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("ALREADY_ISSUED"))
        }

        @Test
        fun `인증 없이 발급하면 401 에러가 발생한다`() {
            RestAssured.given()
            .`when`()
                .post("/api/v1/coupons/1/issue")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("UNAUTHORIZED"))
        }
    }

    @Nested
    inner class GetMyCoupons {
        @Test
        fun `내 쿠폰 목록을 조회할 수 있다`() {
            registerUser()
            val couponId = createCoupon()
            issueCoupon(couponId)

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .get("/api/v1/coupons/me")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.size()", greaterThanOrEqualTo(1))
                .body("data[0].status", equalTo("AVAILABLE"))
        }

        @Test
        fun `인증 없이 조회하면 401 에러가 발생한다`() {
            RestAssured.given()
            .`when`()
                .get("/api/v1/coupons/me")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("UNAUTHORIZED"))
        }
    }

    private fun registerUser() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(
                mapOf(
                    "loginId" to LOGIN_ID,
                    "password" to PASSWORD,
                    "name" to "테스트",
                    "birthDate" to "1993-04-01",
                    "email" to "test@example.com",
                    "gender" to "MALE",
                ),
            )
        .`when`()
            .post("/api/v1/users")
        .then()
            .statusCode(HttpStatus.CREATED.value())
    }

    private fun createCoupon(): Long {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(
                mapOf(
                    "name" to "테스트쿠폰",
                    "discountType" to "FIXED",
                    "discountValue" to 3000,
                    "minOrderAmount" to 0,
                    "maxIssueCount" to 100,
                    "expiredAt" to ZonedDateTime.now().plusDays(30).toString(),
                ),
            )
        .`when`()
            .post("/api-admin/v1/coupons")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .jsonPath()
            .getLong("data.id")
    }

    private fun issueCoupon(couponId: Long) {
        RestAssured.given()
            .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
            .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
        .`when`()
            .post("/api/v1/coupons/$couponId/issue")
        .then()
            .statusCode(HttpStatus.CREATED.value())
    }

    companion object {
        private const val LOGIN_ID = "testuser"
        private const val PASSWORD = "Password1!"
    }
}
